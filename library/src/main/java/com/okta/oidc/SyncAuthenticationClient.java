package com.okta.oidc;

import android.app.Activity;
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
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

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

    @WorkerThread
    public AuthorizationResult logIn(@NonNull final Activity activity,
                                     @Nullable AuthenticationPayload payload)
            throws InterruptedException {
        if (obtainNewConfiguration(mOktaState.getProviderConfiguration(), mOIDCAccount)) {
            ConfigurationRequest request = configurationRequest();
            ProviderConfiguration configuration;
            try {
                configuration = request.executeRequest();
            } catch (AuthorizationException e) {
                return AuthorizationResult.error(e);
            }
            mOktaState.save(configuration);
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
            return AuthorizationResult.error(INVALID_REDIRECT_URI);
        }
        AtomicReference<OktaResultFragment.Result> resultWrapper = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        OktaResultFragment.createLoginFragment(request, mCustomTabColor,
                activity, result -> {
                    resultWrapper.set(result);
                    latch.countDown();
                }, mSupportedBrowsers);
        latch.await();
        OktaResultFragment.Result authResult = resultWrapper.get();
        switch (authResult.getStatus()) {
            case CANCELED:
                return AuthorizationResult.error(USER_CANCELED_AUTH_FLOW);
            case ERROR:
                return AuthorizationResult.error(authResult.getException());
            case AUTHORIZED:
                AuthorizationException exception = validateResult(authResult.getAuthorizationResponse());
                if (exception != null) {
                    return AuthorizationResult.error(exception);
                }
                TokenResponse response;
                try {
                    response = tokenExchange(
                            (AuthorizeResponse) authResult.getAuthorizationResponse())
                            .executeRequest();
                } catch (AuthorizationException e) {
                    return AuthorizationResult.error(e);
                }
                mOktaState.save(response);
                return AuthorizationResult.success(new Tokens(response));
        }
        throw new IllegalStateException("login performed in illegal states");
    }

    private boolean obtainNewConfiguration(ProviderConfiguration providerConfiguration, OIDCAccount account) {
        return (providerConfiguration == null || !providerConfiguration.issuer.equals(account.getDiscoveryUri()));
    }

    public boolean isLoggedIn() {
        TokenResponse tokenResponse = mOktaState.getTokenResponse();
        return tokenResponse != null && tokenResponse.isLoggedIn();
    }

    public Tokens getTokens() {
        TokenResponse response = mOktaState.getTokenResponse();
        if (response == null) return null;
        return new Tokens(response);
    }

    @AnyThread
    public Result signOutFromOkta(@NonNull final Activity activity) {
        if (isLoggedIn()) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OktaResultFragment.Result> resultWrapper = new AtomicReference<>();

            WebRequest request = new LogoutRequest.Builder()
                    .provideConfiguration(mOktaState.getProviderConfiguration())
                    .account(mOIDCAccount)
                    .tokenResponse(mOktaState.getTokenResponse())
                    .state(CodeVerifierUtil.generateRandomState())
                    .create();
            mOktaState.save(request);

            OktaResultFragment.createLogoutFragment(request, mCustomTabColor,
                    activity, result -> {
                        resultWrapper.set(result);
                        latch.countDown();
                    }, mSupportedBrowsers);
            OktaResultFragment.Result logoutResult = resultWrapper.get();

            switch (logoutResult.getStatus()) {
                case CANCELED:
                    return Result.error(INVALID_REDIRECT_URI);
                case ERROR:
                    return Result.error(logoutResult.getException());
                case LOGGED_OUT:
                    mOktaState.delete(mOktaState.getAuthorizeRequest());
                    return Result.success();
            }
        }
        return Result.success();
    }

    private AuthorizationException validateResult(WebResponse authResponse) {
        WebRequest authorizedRequest = mOktaState.getAuthorizeRequest();
        if (authorizedRequest == null) {
            return USER_CANCELED_AUTH_FLOW;
        }

        String requestState = authorizedRequest.getState();
        String responseState = authResponse.getState();
        if (requestState == null && responseState != null
                || (requestState != null && !requestState
                .equals(responseState))) {
            return AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH;
        }
        return null;
    }

    @WorkerThread
    private TokenRequest tokenExchange(AuthorizeResponse response) {
        return (TokenRequest) HttpRequestBuilder.newRequest()
                .request(TOKEN_EXCHANGE)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount)
                .authRequest((AuthorizeRequest) mOktaState.getAuthorizeRequest())
                .authResponse(response)
                .createRequest();
    }

    private boolean isRedirectUrisRegistered(@NonNull Uri uri, Activity activity) {
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
}
