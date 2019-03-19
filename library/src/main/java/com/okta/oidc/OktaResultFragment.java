package com.okta.oidc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.LogoutResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;

import static android.app.Activity.RESULT_CANCELED;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;

public class OktaResultFragment extends Fragment {

    private static final String AUTHENTICATION_REQUEST = "authRequest";
    private static final String CUSTOM_COLOR = "customColor";

    public static final String REQUEST_STATUS_KEY = "requestStatus";

    public static final int REQUEST_CODE_SIGN_IN = 100;
    public static final int REQUEST_CODE_SIGN_OUT = 200;

    private AuthenticateClient client;
    private Result cashedResult;
    private boolean requestInProgress;

    public static void createLoginFragment(WebRequest request,
                                           int customColor,
                                           FragmentActivity activity,
                                           AuthenticateClient client) {

        OktaResultFragment fragment = new OktaResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(AUTHENTICATION_REQUEST, request.toUri());
        bundle.putInt(CUSTOM_COLOR, customColor);
        fragment.setArguments(bundle);
        fragment.setAuthenticatioClient(client);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, AUTHENTICATION_REQUEST)
                .commitNow();
    }

    public static void setAuthenticationClient(FragmentActivity activity,
                                               AuthenticateClient client) {

        OktaResultFragment resultFragment = (OktaResultFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST);
        if (resultFragment != null) {
            resultFragment.setAuthenticatioClient(client);
        }
    }

    public static boolean hasRequestInProgress(FragmentActivity activity){
        return activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST) != null;
    }

    private void setAuthenticatioClient(AuthenticateClient client) {
        this.client = client;
        postResult();
    }

    private void postResult() {
        if (cashedResult != null && client != null) {
            client.postResult(cashedResult);
            cashedResult = null;
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitNow();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            requestInProgress = savedInstanceState.getBoolean(REQUEST_STATUS_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(REQUEST_STATUS_KEY, requestInProgress);
        super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!requestInProgress) {
            Intent intent = null;
            if (getArguments().getParcelable(AUTHENTICATION_REQUEST) != null) {
                intent = createAuthIntent(getArguments().getParcelable(AUTHENTICATION_REQUEST),
                        getArguments().getInt(CUSTOM_COLOR));
            }
            startActivityForResult(intent, REQUEST_CODE_SIGN_IN);
            requestInProgress = true;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private Intent createAuthIntent(Uri request, int customColor) {
        Intent intent = new Intent(getActivity(), OktaAuthenticationActivity.class);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_AUTH_URI, request);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_TAB_OPTIONS, customColor);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        requestInProgress = false;
    }

    private Result retrieveResponse(Uri responseUri, int requestCode) {
        if (responseUri.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
            return Result.exception(AuthorizationException.fromOAuthRedirect(responseUri));
        } else {
            WebResponse response = requestCode == REQUEST_CODE_SIGN_IN ?
                    AuthorizeResponse.fromUri(responseUri) : LogoutResponse.fromUri(responseUri);
            return Result.success(response);
        }
    }

    public enum Status {
        CANCELED, ERROR, SUCCESS
    }

    public static class Result {

        private AuthorizationException exception;
        private WebResponse authorizationResponse;
        private Status status;

        public static Result canceled() {
            return new Result(null, null, Status.CANCELED);
        }

        public static Result success(WebResponse response) {
            return new Result(null, response, Status.SUCCESS);
        }

        public static Result exception(AuthorizationException exception) {
            return new Result(exception, null, Status.SUCCESS);
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

        public void setAuthorizationResponse(WebResponse authorizationResponse) {
            this.authorizationResponse = authorizationResponse;
        }

        public Status getStatus() {
            return status;
        }
    }

}
