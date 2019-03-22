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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.AnyThread;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.request.TokenRequest;

import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.request.web.LogoutRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.okta.oidc.net.request.HttpRequest.Type.TOKEN_EXCHANGE;
import static com.okta.oidc.util.AuthorizationException.RegistrationRequestErrors.INVALID_REDIRECT_URI;

public final class AuthenticateClient {
    private static final String TAG = AuthenticateClient.class.getSimpleName();
    private static final String AUTH_REQUEST_PREF = "AuthRequest";
    private static final String AUTH_RESPONSE_PREF = "AuthResponse";
    //need to restore auth.
    private static final String AUTH_RESTORE_PREF = AuthenticateClient.class.getCanonicalName() + ".AuthRestore";

    private WeakReference<FragmentActivity> mActivity;
    private OIDCAccount mOIDCAccount;
    private Map<String, String> mAdditionalParams;
    private int mCustomTabColor;
    private String mState;
    private String mLoginHint;
    private OktaRepository mOktaRepo;

    private RequestDispatcher mDispatcher;
    private WebRequest mAuthorizeRequest;
    private WebResponse mAuthResponse;

    private HttpConnectionFactory mConnectionFactory;
    private ResultCallback<AuthorisationStatus, AuthorizationException> mResultCb;
    private HttpRequest mCurrentHttpRequest;
    public static final int REQUEST_CODE_SIGN_IN = 100;
    public static final int REQUEST_CODE_SIGN_OUT = 101;
    //Hold the exception to send to onActivityResult
    private AuthorizationException mErrorActivityResult;

    private AuthenticateClient(@NonNull Builder builder) {
        mConnectionFactory = builder.mConnectionFactory;
        mOIDCAccount = builder.mOIDCAccount;
        mCustomTabColor = builder.mCustomTabColor;
        mAdditionalParams = builder.mAdditionalParams;
        mState = builder.mState;
        mLoginHint = builder.mLoginHint;
        mOktaRepo = builder.mOktaRepo;
        mDispatcher = new RequestDispatcher(builder.mCallbackExecutor);
    }

    public void registerCallback(ResultCallback<AuthorisationStatus, AuthorizationException> resultCallback, FragmentActivity activity) {
        mResultCb = resultCallback;
        registerActivityLifeCycle(activity);
        if (OktaResultFragment.hasRequestInProgress(activity)) {
            OktaResultFragment.setAuthenticationClient(activity, this);
        }
    }

    private void registerActivityLifeCycle(@NonNull final FragmentActivity activity) {
        mActivity = new WeakReference<>(activity);
        mActivity.get().getApplication().registerActivityLifecycleCallbacks(new EmptyActivityLifeCycle() {
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                if (mActivity != null && mActivity.get() == activity) {
                    persist();
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (mActivity != null && mActivity.get() == activity) {
                    stop();
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }
        });
    }

    private void persist() {
        mOktaRepo.save(mAuthorizeRequest);
    }

    private void restore() {
        mAuthorizeRequest = (WebRequest) mOktaRepo.restore(WebRequest.RESTORE);
    }

    private void clearPreferences() {
        mOktaRepo.delete(mAuthorizeRequest);
        mOktaRepo.delete(mAuthResponse);

    }

    private void cancelCurrentRequest() {
        if (mCurrentHttpRequest != null) {
            mCurrentHttpRequest.cancelRequest();
            mCurrentHttpRequest = null;
        }
    }

    public ConfigurationRequest configurationRequest() {
        cancelCurrentRequest();
        mCurrentHttpRequest = HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.CONFIGURATION)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCAccount).createRequest();
        return (ConfigurationRequest) mCurrentHttpRequest;
    }

    public AuthorizedRequest userProfileRequest() {
        cancelCurrentRequest();
        mCurrentHttpRequest = HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.PROFILE)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCAccount).createRequest();
        return (AuthorizedRequest) mCurrentHttpRequest;
    }

    public RevokeTokenRequest revokeTokenRequest(String token) {
        cancelCurrentRequest();
        mCurrentHttpRequest = HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REVOKE_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenToRevoke(token)
                .account(mOIDCAccount).createRequest();
        return (RevokeTokenRequest) mCurrentHttpRequest;
    }

    public AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters,
                                               @NonNull HttpConnection.RequestMethod method) {
        cancelCurrentRequest();
        mCurrentHttpRequest = HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.AUTHORIZED)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCAccount)
                .uri(uri)
                .properties(properties)
                .postParameters(postParameters)
                .createRequest();
        return (AuthorizedRequest) mCurrentHttpRequest;
    }

    public void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb) {
        AuthorizedRequest request = userProfileRequest();
        request.dispatchRequest(mDispatcher, cb);
    }

    public void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb) {
        RevokeTokenRequest request = revokeTokenRequest(token);
        request.dispatchRequest(mDispatcher, cb);
    }

    @AnyThread
    public void logIn(@NonNull final FragmentActivity activity) {
        if (mOIDCAccount.obtainNewConfiguration()) {
            ConfigurationRequest request = configurationRequest();
            mCurrentHttpRequest = request;
            request.dispatchRequest(mDispatcher, new RequestCallback<ProviderConfiguration, AuthorizationException>() {
                @Override
                public void onSuccess(@NonNull ProviderConfiguration result) {
                    mOktaRepo.save(result);
                    mOIDCAccount.setProviderConfig(result);
                    authorizationRequest(activity);
                }

                @Override
                public void onError(String error, AuthorizationException exception) {
                    Log.w(TAG, "can't obtain discovery doc", exception);
                    mErrorActivityResult = exception;
                    //Authorize anyways the error will be passed to app.
                    authorizationRequest(activity);
                }
            });
        } else {
            authorizationRequest(activity);
        }
    }

    @AnyThread
    public void logOut(@NonNull final FragmentActivity activity) {
        if (mOIDCAccount.isLoggedIn()) {
            registerActivityLifeCycle(activity);
            mAuthorizeRequest = new LogoutRequest.Builder().account(mOIDCAccount)
                    .state(CodeVerifierUtil.generateRandomState())
                    .create();
            OktaResultFragment.createLogouttFragment(mAuthorizeRequest, mCustomTabColor,
                    activity, this);
        }
    }

    private void authorizationRequest(FragmentActivity activity) {
        registerActivityLifeCycle(activity);
        if (mOIDCAccount.obtainNewConfiguration()) {
            mAuthorizeRequest = createAuthorizeRequest();
            if (!isRedirectUrisRegistered(mOIDCAccount.getRedirectUri())) {
                Log.e(TAG, "No uri registered to handle redirect or multiple applications registered");
                //FIXME move error to listener
                mErrorActivityResult = INVALID_REDIRECT_URI;
            }
            OktaResultFragment.createLoginFragment(mAuthorizeRequest, mCustomTabColor,
                    activity, this);
        } else {
            mErrorActivityResult = AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT;
        }
    }

    void postResult(OktaResultFragment.Result result) {
        if (mResultCb != null) {
            switch (result.getStatus()) {
                case CANCELED:
                    mResultCb.onCancel();
                    break;
                case ERROR:
                    mResultCb.onError("Login error", result.getException());
                    break;
                case AUTHORIZED:
                    if (validateResult(result.getAuthorizationResponse())) {
                        tokenExchange();
                    }
                    break;
                case LOGGED_OUT:
                    clearPreferences();
                    mOIDCAccount = null;
                    m
                    mResultCb.onSuccess(AuthorisationStatus.LOGGED_OUT);
                    break;
            }
        }
    }

    private boolean validateResult(WebResponse authResponse) {
        mAuthResponse = authResponse;
        if (mAuthorizeRequest == null && mActivity.get() != null) {
            restore();
            if (mAuthorizeRequest == null) {
                mResultCb.onError("Response error", AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW);
                return false;
            }
        }

        String requestState = mAuthorizeRequest.getState();
        String responseState = authResponse.getState();
        if (requestState == null && responseState != null
                || (requestState != null && !requestState
                .equals(responseState))) {
            mResultCb.onError("Mismatch states", AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH);
            return false;
        }
        return true;
    }


    private AuthorizeRequest createAuthorizeRequest() {
        AuthorizeRequest.Builder builder = new AuthorizeRequest.Builder();
        builder.account(mOIDCAccount);
        if (mAdditionalParams != null) {
            builder.additionalParams(mAdditionalParams);
        }
        if (!TextUtils.isEmpty(mState)) {
            builder.state(mState);
        }
        if (!TextUtils.isEmpty(mLoginHint)) {
            builder.state(mLoginHint);
        }
        return builder.create();
    }

    private void stop() {
        mResultCb = null;
        cancelCurrentRequest();
        mDispatcher.shutdown();
    }

    @WorkerThread
    private void tokenExchange() {
        mCurrentHttpRequest = HttpRequestBuilder.newRequest().request(TOKEN_EXCHANGE).account(mOIDCAccount)
                .authRequest((AuthorizeRequest) mAuthorizeRequest)
                .authResponse((AuthorizeResponse) mAuthResponse)
                .createRequest();

        ((TokenRequest) mCurrentHttpRequest).dispatchRequest(mDispatcher, new RequestCallback<TokenResponse, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull TokenResponse result) {
                mOktaRepo.save(result);
                mOIDCAccount.setTokenResponse(result);
                mResultCb.onSuccess(AuthorisationStatus.AUTHORIZED);
            }

            @Override
            public void onError(String error, AuthorizationException exception) {
                mResultCb.onError("Failed to complete exchange request", exception);
            }
        });
    }

    private boolean isRedirectUrisRegistered(@NonNull Uri uri) {
        PackageManager pm = mActivity.get().getPackageManager();
        List<ResolveInfo> resolveInfos = null;
        if (pm != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(uri);
            resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        }
        boolean found = false;
        if (resolveInfos != null) {
            for (ResolveInfo info : resolveInfos) {
                ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo.name.equals(OktaRedirectActivity.class.getCanonicalName()) &&
                        activityInfo.packageName.equals(mActivity.get().getPackageName())) {
                    found = true;
                } else {
                    Log.w(TAG, "Warning! Multiple " +
                            "applications found registered with same scheme");
                    //Another installed app have same url scheme.
                    //return false as if no activity found to prevent hijacking of redirect.
                    return false;
                }
            }
        }
        return found;
    }

    public static final class Builder {
        private Executor mCallbackExecutor;
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private Map<String, String> mAdditionalParams;
        private int mCustomTabColor;
        private String mState;
        private String mLoginHint;
        private OktaRepository mOktaRepo;

        public Builder() {
        }

        public AuthenticateClient create() {
            ProviderConfiguration configuration = (ProviderConfiguration) mOktaRepo.restore(ProviderConfiguration.RESTORE);
            TokenResponse response = (TokenResponse) mOktaRepo.restore(TokenResponse.RESTORE);
            mOIDCAccount.setProviderConfig(configuration);
            mOIDCAccount.setTokenResponse(response);
            return new AuthenticateClient(this);
        }

        public Builder withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        public Builder withParameters(@NonNull Map<String, String> parameters) {
            mAdditionalParams = parameters;
            return this;
        }

        public Builder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        public Builder withStorage(OktaStorage storage, Context context) {
            mOktaRepo = new OktaRepository(storage, context);
            return this;
        }

        public Builder withState(@NonNull String state) {
            mState = state;
            return this;
        }

        public Builder withLoginHint(@NonNull String loginHint) {
            mLoginHint = loginHint;
            return this;
        }

        public Builder callbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        public Builder httpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

    }
}