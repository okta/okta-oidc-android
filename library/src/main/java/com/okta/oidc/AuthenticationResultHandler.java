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

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.LogoutResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;

import static android.app.Activity.RESULT_CANCELED;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;
import static com.okta.oidc.OktaResultFragment.REQUEST_CODE_SIGN_IN;
import static com.okta.oidc.OktaResultFragment.REQUEST_CODE_SIGN_OUT;

/**
 * @hide Handle the call back data from chrome custom tabs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthenticationResultHandler {
    @VisibleForTesting
    public AuthResultListener mResultListener;
    @VisibleForTesting
    public StateResult mCachedResult;
    @VisibleForTesting
    public ResultType mCachedResultType;

    private static AuthenticationResultHandler sHandler = new AuthenticationResultHandler();

    private AuthenticationResultHandler() {
    }

    public static AuthenticationResultHandler handler() {
        return sHandler;
    }

    public void setAuthenticationListener(AuthResultListener listener) {
        mResultListener = listener;
        postResult();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_SIGN_IN && requestCode != REQUEST_CODE_SIGN_OUT) {
            return;
        }
        mCachedResultType = (requestCode == REQUEST_CODE_SIGN_IN) ? ResultType.SIGN_IN
                : ResultType.SIGN_OUT;

        mCachedResult = handleAuthenticationResult(requestCode, resultCode, data);
        postResult();
    }

    private StateResult handleAuthenticationResult(int requestCode, int resultCode, Intent data) {
        StateResult result;
        if (resultCode == RESULT_CANCELED) {
            result = StateResult.canceled();
        } else {
            Uri response = data.getData();
            if (response != null) {
                result = retrieveResponse(response, requestCode);
            } else {
                try {
                    result = StateResult.exception(AuthorizationException
                            .fromJson(data.getExtras().getString(EXTRA_EXCEPTION, "")));
                } catch (NullPointerException | IllegalArgumentException e) {
                    result = StateResult.exception(
                            AuthorizationException.AuthorizationRequestErrors.OTHER);
                } catch (JSONException je) {
                    result = StateResult.exception(
                            AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR);
                }
            }
        }
        return result;
    }

    private StateResult retrieveResponse(Uri responseUri, int requestCode) {
        if (responseUri.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
            return StateResult.exception(AuthorizationException.fromOAuthRedirect(responseUri));
        } else {
            if (requestCode == REQUEST_CODE_SIGN_IN) {
                return StateResult.authorized(AuthorizeResponse.fromUri(responseUri));
            } else if (requestCode == REQUEST_CODE_SIGN_OUT) {
                return StateResult.loggedOut(LogoutResponse.fromUri(responseUri));
            }
        }
        return StateResult.exception(new AuthorizationException("Unknown response from browser",
                new IllegalStateException()));
    }

    private void postResult() {
        if (mCachedResult != null && mResultListener != null) {
            mResultListener.postResult(mCachedResult, mCachedResultType);
            mCachedResult = null;
            mCachedResultType = null;
        }
    }

    public enum Status {
        CANCELED, ERROR, AUTHORIZED, LOGGED_OUT
    }

    public enum ResultType {
        SIGN_IN,
        SIGN_OUT
    }

    public interface AuthResultListener {
        void postResult(StateResult result, ResultType type);
    }

    public static class StateResult {

        private AuthorizationException exception;
        private WebResponse authorizationResponse;
        private Status status;

        public static StateResult canceled() {
            return new StateResult(null, null, Status.CANCELED);
        }

        public static StateResult authorized(WebResponse response) {
            return new StateResult(null, response, Status.AUTHORIZED);
        }

        public static StateResult loggedOut(WebResponse response) {
            return new StateResult(null, response, Status.LOGGED_OUT);
        }

        public static StateResult exception(AuthorizationException exception) {
            return new StateResult(exception, null, Status.ERROR);
        }

        public StateResult(AuthorizationException exception, WebResponse response, Status status) {
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
}
