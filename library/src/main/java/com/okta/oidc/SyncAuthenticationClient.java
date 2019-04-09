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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.LogoutRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;

import static com.okta.oidc.net.request.HttpRequest.Type.TOKEN_EXCHANGE;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW;
import static com.okta.oidc.util.AuthorizationException.RegistrationRequestErrors.INVALID_REDIRECT_URI;

public class SyncAuthenticationClient {
    private static final String TAG = AuthenticateClient.class.getSimpleName();

    private OIDCAccount mOIDCAccount;
    private int mCustomTabColor;
    OktaState mOktaState;
    private String[] mSupportedBrowsers;
    private HttpConnectionFactory mConnectionFactory;

    SyncAuthenticationClient(HttpConnectionFactory factory, OIDCAccount account,
                             int customTabColor, OktaStorage storage,
                             Context context, String[] browsers) {
        mConnectionFactory = factory;
        mOIDCAccount = account;
        mCustomTabColor = customTabColor;
        mOktaState = new OktaState(new OktaRepository(storage, context));
        mSupportedBrowsers = browsers;
    }

    public void clear() {
        mOktaState.delete(mOktaState.getProviderConfiguration());
        mOktaState.delete(mOktaState.getTokenResponse());
        mOktaState.delete(mOktaState.getAuthorizeRequest());
        resetCurrentState();
    }

    public NativeAuthorizeRequest nativeAuthorizeRequest(String sessionToken,
                                                         AuthenticationPayload payload) {
        return new AuthorizeRequest.Builder()
                .account(mOIDCAccount)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .sessionToken(sessionToken)
                .authenticationPayload(payload)
                .createNativeRequest(mConnectionFactory);
    }

    public ConfigurationRequest configurationRequest() {
        return (ConfigurationRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.CONFIGURATION)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCAccount).createRequest();
    }

    public AuthorizedRequest userProfileRequest() {
        return (AuthorizedRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.PROFILE)
                .connectionFactory(mConnectionFactory)
                .tokenResponse(mOktaState.getTokenResponse())
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    public RevokeTokenRequest revokeTokenRequest(String token) {
        return (RevokeTokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REVOKE_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenToRevoke(token)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    public RefreshTokenRequest refreshTokenRequest() {
        return (RefreshTokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REFRESH_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenResponse(mOktaState.getTokenResponse())
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    public IntrospectRequest introspectTokenRequest(String token, String tokenType) {
        return (IntrospectRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.INTROSPECT)
                .connectionFactory(mConnectionFactory)
                .introspect(token, tokenType)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    public AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters,
                                               @NonNull HttpConnection.RequestMethod method) {
        return (AuthorizedRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.AUTHORIZED)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCAccount)
                .httpRequestMethod(method)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .tokenResponse(mOktaState.getTokenResponse())
                .uri(uri)
                .properties(properties)
                .postParameters(postParameters)
                .createRequest();
    }

    private void obtainNewConfiguration() throws AuthorizationException {
        ProviderConfiguration config = mOktaState.getProviderConfiguration();
        if (config == null || !config.issuer.equals(mOIDCAccount.getDiscoveryUri())) {
            mOktaState.setCurrentState(State.OBTAIN_CONFIGURATION);
            mOktaState.save(configurationRequest().executeRequest());
        }
    }

    @WorkerThread
    public AuthorizationResult logInNative(String sessionToken,
                                           @Nullable AuthenticationPayload payload) {
        try {
            obtainNewConfiguration();
            mOktaState.setCurrentState(State.SIGN_IN_REQUEST);
            NativeAuthorizeRequest request = nativeAuthorizeRequest(sessionToken, payload);
            //FIXME Need to the parameters of native request in a web request because
            //oktaState uses it to verify the returned response.
            AuthorizeRequest authRequest = new AuthorizeRequest(request.getParameters());
            mOktaState.save(authRequest);
            AuthorizeResponse authResponse = request.executeRequest();
            validateResult(authResponse);
            mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
            TokenResponse tokenResponse = tokenExchange(authResponse).executeRequest();
            mOktaState.save(tokenResponse);
            return AuthorizationResult.success(new Tokens(tokenResponse));
        } catch (AuthorizationException e) {
            return AuthorizationResult.error(e);
        } finally {
            resetCurrentState();
        }
    }

    @WorkerThread
    public AuthorizationResult logIn(@NonNull final FragmentActivity activity,
                                     @Nullable AuthenticationPayload payload)
            throws InterruptedException {
        try {
            obtainNewConfiguration();
        } catch (AuthorizationException e) {
            resetCurrentState();
            return AuthorizationResult.error(e);
        }

        WebRequest request = new AuthorizeRequest.Builder()
                .account(mOIDCAccount)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .authenticationPayload(payload)
                .create();

        mOktaState.save(request);
        if (!isRedirectUrisRegistered(mOIDCAccount.getRedirectUri(), activity)) {
            Log.e(TAG, "No uri registered to handle redirect or multiple applications registered");
            //FIXME move error to listener
            resetCurrentState();
            return AuthorizationResult.error(INVALID_REDIRECT_URI);
        }
        mOktaState.setCurrentState(State.SIGN_IN_REQUEST);
        AtomicReference<OktaResultFragment.Result> resultWrapper = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        OktaResultFragment.addLoginFragment(request, mCustomTabColor,
                activity, (result, type) -> {
                    resultWrapper.set(result);
                    latch.countDown();
                }, mSupportedBrowsers);
        latch.await();
        OktaResultFragment.Result authResult = resultWrapper.get();
        AuthorizationResult result = processLogInResult(authResult);
        resetCurrentState();
        if (result == null) {
            throw new IllegalStateException("login performed in illegal states");
        }

        return result;
    }

    private AuthorizationResult processLogInResult(OktaResultFragment.Result result) {
        switch (result.getStatus()) {
            case CANCELED:
                return AuthorizationResult.cancel();
            case ERROR:
                return AuthorizationResult.error(result.getException());
            case AUTHORIZED:
                mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
                TokenResponse response;
                try {
                    validateResult(result.getAuthorizationResponse());
                    response = tokenExchange(
                            (AuthorizeResponse) result.getAuthorizationResponse())
                            .executeRequest();
                } catch (AuthorizationException e) {
                    return AuthorizationResult.error(e);
                }
                mOktaState.save(response);
                return AuthorizationResult.success(new Tokens(response));
        }
        return null;
    }


    public boolean isLoggedIn() {
        TokenResponse tokenResponse = mOktaState.getTokenResponse();
        return tokenResponse != null &&
                (tokenResponse.getAccessToken() != null || tokenResponse.getIdToken() != null);
    }

    public Tokens getTokens() {
        TokenResponse response = mOktaState.getTokenResponse();
        if (response == null) return null;
        return new Tokens(response);
    }

    public State getCurrentState() {
        return mOktaState.getCurrentState();
    }

    @AnyThread
    public Result signOutFromOkta(@NonNull final FragmentActivity activity)
            throws InterruptedException {
        if (isLoggedIn()) {
            mOktaState.setCurrentState(State.SIGN_OUT_REQUEST);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OktaResultFragment.Result> resultWrapper = new AtomicReference<>();
            WebRequest request = new LogoutRequest.Builder()
                    .provideConfiguration(mOktaState.getProviderConfiguration())
                    .account(mOIDCAccount)
                    .tokenResponse(mOktaState.getTokenResponse())
                    .state(CodeVerifierUtil.generateRandomState())
                    .create();
            mOktaState.save(request);
            OktaResultFragment.addLogoutFragment(request, mCustomTabColor,
                    activity, (result, type) -> {
                        resultWrapper.set(result);
                        latch.countDown();
                    }, mSupportedBrowsers);
            latch.await();
            OktaResultFragment.Result logoutResult = resultWrapper.get();
            Result result = processSignOutResult(logoutResult);
            resetCurrentState();
            if (result != null) {
                return result;
            }

        }
        resetCurrentState();
        return Result.success();
    }


    private Result processSignOutResult(OktaResultFragment.Result result) {
        switch (result.getStatus()) {
            case CANCELED:
                return Result.error(INVALID_REDIRECT_URI);
            case ERROR:
                return Result.error(result.getException());
            case LOGGED_OUT:
                return Result.success();
        }
        return null;
    }

    protected void registerCallbackIfInterrupt(FragmentActivity activity, ResultListener resultListener, ExecutorService executorService) {
        if (OktaResultFragment.hasRequestInProgress(activity)) {
            OktaResultFragment.getFragment(activity).setAuthenticationListener((result, type) -> {
                executorService.execute(() -> {
                    switch (type) {
                        case SIGN_IN:
                            AuthorizationResult authorizationResult = processLogInResult(result);
                            resetCurrentState();
                            resultListener.postResult(authorizationResult, type);
                            break;
                        case SIGN_OUT:
                            Result signOutResult = processSignOutResult(result);
                            resetCurrentState();
                            resultListener.postResult(signOutResult, type);
                            break;
                    }
                });
            });
        }
    }

    private void validateResult(WebResponse authResponse) throws AuthorizationException {
        WebRequest authorizedRequest = mOktaState.getAuthorizeRequest();
        if (authorizedRequest == null) {
            throw USER_CANCELED_AUTH_FLOW;
        }

        String requestState = authorizedRequest.getState();
        String responseState = authResponse.getState();
        if (requestState == null && responseState != null
                || (requestState != null && !requestState
                .equals(responseState))) {
            throw AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH;
        }
    }

    @WorkerThread
    protected TokenRequest tokenExchange(AuthorizeResponse response) {
        return (TokenRequest) HttpRequestBuilder.newRequest()
                .request(TOKEN_EXCHANGE)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount)
                .authRequest((AuthorizeRequest) mOktaState.getAuthorizeRequest())
                .authResponse(response)
                .createRequest();
    }

    private boolean isRedirectUrisRegistered(@NonNull Uri uri, FragmentActivity activity) {
        PackageManager pm = activity.getPackageManager();
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
                        activityInfo.packageName.equals(activity.getPackageName())) {
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

    private void resetCurrentState() {
        mOktaState.setCurrentState(State.IDLE);
    }

    public interface ResultListener {
        void postResult(Result result, OktaResultFragment.ResultType resultType);
    }

}
