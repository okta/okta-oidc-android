package com.okta.oidc;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.LogoutResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;

import static android.app.Activity.RESULT_CANCELED;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;

public class OktaResultFragment extends Fragment {
    static final String AUTHENTICATION_REQUEST = "authRequest";

    public static final int REQUEST_CODE_SIGN_IN = 100;
    public static final int REQUEST_CODE_SIGN_OUT = 200;

    private AuthResultListener resultListener;
    private Result cashedResult;
    private Intent authIntent;
    private Intent logoutIntent;

    public static void createLoginFragment(WebRequest request,
                                           int customColor,
                                           Activity activity,
                                           AuthResultListener listener, String[] browsers) {

        OktaResultFragment fragment = new OktaResultFragment();
        fragment.setAuthenticationListener(listener);
        fragment.authIntent = fragment.createAuthIntent(activity, request.toUri(), customColor,
                browsers);

        activity.getFragmentManager()
                .beginTransaction()
                .add(fragment, AUTHENTICATION_REQUEST)
                .commit();
    }

    public static void createLogoutFragment(WebRequest request,
                                            int customColor,
                                            Activity activity,
                                            AuthResultListener listener,
                                            String[] browsers) {

        OktaResultFragment fragment = new OktaResultFragment();
        fragment.setAuthenticationListener(listener);
        fragment.logoutIntent = fragment.createAuthIntent(activity, request.toUri(), customColor,
                browsers);

        activity.getFragmentManager().beginTransaction()
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
        }
        super.onResume();
    }

    public static void setAuthenticationListener(Activity activity,
                                                 AuthResultListener listener) {

        OktaResultFragment resultFragment = (OktaResultFragment) activity.getFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST);
        if (resultFragment != null) {
            resultFragment.setAuthenticationListener(listener);
        }
    }

    public static boolean hasRequestInProgress(Activity activity) {
        return activity.getFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST) != null;
    }

    private void setAuthenticationListener(AuthResultListener listener) {
        this.resultListener = listener;
        postResult();
    }

    private void postResult() {
        if (cashedResult != null && resultListener != null) {
            resultListener.postResult(cashedResult);
            cashedResult = null;
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
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
        if ((requestCode == REQUEST_CODE_SIGN_IN || requestCode == REQUEST_CODE_SIGN_OUT)) {
            if (resultCode == RESULT_CANCELED) {
                cashedResult = Result.canceled();
            } else {
                Uri response = data.getData();
                if (response != null) {
                    cashedResult = retrieveResponse(response, requestCode);
                } else {
                    Bundle bundle = data.getExtras();
                    if (bundle != null) {
                        String json = bundle.getString(EXTRA_EXCEPTION, null);
                        if (json != null) {
                            try {
                                AuthorizationException exception = AuthorizationException.fromJson(json);
                                cashedResult = Result.exception(exception);
                            } catch (JSONException e) {
                                cashedResult = Result.exception(AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR);
                            }
                        }
                    } else {
                        cashedResult = Result.exception(AuthorizationException.GeneralErrors.INVALID_REGISTRATION_RESPONSE);
                    }
                }
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
                return Result.loggeout(LogoutResponse.fromUri(responseUri));
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

        public static Result loggeout(WebResponse response) {
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
        void postResult(Result result);
    }

}
