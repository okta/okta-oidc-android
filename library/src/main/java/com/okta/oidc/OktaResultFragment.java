/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.oidc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.LogoutResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;

import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import static android.app.Activity.RESULT_CANCELED;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class OktaResultFragment extends Fragment {
    static final String AUTHENTICATION_REQUEST = "authRequest";

    enum ResultType {
        SIGN_IN,
        SIGN_OUT
    }

    public static final int REQUEST_CODE_SIGN_IN = 100;
    public static final int REQUEST_CODE_SIGN_OUT = 200;

    private AuthResultListener resultListener;
    private Result cachedResult;
    private ResultType cachedResultType;
    private Intent authIntent;
    private Intent logoutIntent;

    static void addLoginFragment(WebRequest request,
                                 int customColor,
                                 FragmentActivity activity,
                                 AuthResultListener listener, String[] browsers) {

        OktaResultFragment fragment = new OktaResultFragment();
        fragment.setAuthenticationListener(listener);
        fragment.authIntent = fragment.createAuthIntent(activity, request.toUri(), customColor,
                browsers);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, AUTHENTICATION_REQUEST)
                .commit();
    }

    static void addLogoutFragment(WebRequest request,
                                  int customColor,
                                  FragmentActivity activity,
                                  AuthResultListener listener,
                                  String[] browsers) {

        OktaResultFragment fragment = new OktaResultFragment();
        fragment.setAuthenticationListener(listener);
        fragment.logoutIntent = fragment.createAuthIntent(activity, request.toUri(), customColor,
                browsers);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, AUTHENTICATION_REQUEST)
                .commit();

    }

    @Override
    public void onResume() {
        if (authIntent != null) {
            startActivityForResult(authIntent, REQUEST_CODE_SIGN_IN);
            authIntent = null;
        }
        if (logoutIntent != null) {
            startActivityForResult(logoutIntent, REQUEST_CODE_SIGN_OUT);
            logoutIntent = null;
        }
        super.onResume();
    }

    public static void setAuthenticationListener(FragmentActivity activity,
                                                 AuthResultListener listener) {

        OktaResultFragment resultFragment = (OktaResultFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST);
        if (resultFragment != null) {
            resultFragment.setAuthenticationListener(listener);
        }
    }

    public static boolean hasRequestInProgress(FragmentActivity activity) {
        return activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST) != null;
    }

    public static OktaResultFragment getFragment(FragmentActivity activity) {
        return (OktaResultFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST);
    }

    public void setAuthenticationListener(AuthResultListener listener) {
        this.resultListener = listener;
        postResult();
    }

    private void postResult() {
        if (cachedResult != null && resultListener != null) {
            resultListener.postResult(cachedResult, cachedResultType);
            cachedResult = null;
            cachedResultType = null;
        }
    }

    private Intent createAuthIntent(Activity activity, Uri request, int customColor, String[] browsers) {
        Intent intent = new Intent(activity, OktaAuthenticationActivity.class);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_BROWSERS, browsers);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_AUTH_URI, request);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_TAB_OPTIONS, customColor);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_SIGN_IN && requestCode != REQUEST_CODE_SIGN_OUT) {
            return;
        }
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitNow();
        cachedResultType = (requestCode == REQUEST_CODE_SIGN_IN) ? ResultType.SIGN_IN : ResultType.SIGN_OUT;

        if (resultCode == RESULT_CANCELED) {
            cachedResult = Result.canceled();
            postResult();
            return;
        }

        Uri response = data.getData();
        if (response != null) {
            cachedResult = retrieveResponse(response, requestCode);
        } else {
            try {
                cachedResult = Result.exception(AuthorizationException.fromJson(data.getExtras()
                        .getString(EXTRA_EXCEPTION, "")));
            } catch (NullPointerException | IllegalArgumentException e) {
                cachedResult = Result.exception(AuthorizationException.AuthorizationRequestErrors.OTHER);
            } catch (JSONException je) {
                cachedResult = Result.exception(AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR);
            }
        }
        postResult();
    }

    private Result retrieveResponse(Uri responseUri, int requestCode) {
        if (responseUri.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
            return Result.exception(AuthorizationException.fromOAuthRedirect(responseUri));
        } else {
            if (requestCode == REQUEST_CODE_SIGN_IN) {
                return Result.authorized(AuthorizeResponse.fromUri(responseUri));
            } else if (requestCode == REQUEST_CODE_SIGN_OUT) {
                return Result.loggedOut(LogoutResponse.fromUri(responseUri));
            }
        }
        throw new IllegalStateException();
    }

    public enum Status {
        CANCELED, ERROR, AUTHORIZED, LOGGED_OUT
    }

    public static class Result {

        private AuthorizationException exception;
        private WebResponse authorizationResponse;
        private Status status;

        public static Result canceled() {
            return new Result(null, null, Status.CANCELED);
        }

        public static Result authorized(WebResponse response) {
            return new Result(null, response, Status.AUTHORIZED);
        }

        public static Result loggedOut(WebResponse response) {
            return new Result(null, response, Status.LOGGED_OUT);
        }

        public static Result exception(AuthorizationException exception) {
            return new Result(exception, null, Status.ERROR);
        }

        public Result(AuthorizationException exception, WebResponse response, Status status) {
            this.exception = exception;
            this.authorizationResponse = response;
            this.status = status;
        }

        public AuthorizationException getException() {
            return exception;
        }

        public WebResponse getAuthorizationResponse() {
            return authorizationResponse;
        }

        public Status getStatus() {
            return status;
        }
    }

    public interface AuthResultListener {
        void postResult(Result result, ResultType type);
    }

}
