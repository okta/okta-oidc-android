package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.State;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SyncSession;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

class SyncNativeAuthClient extends AuthClient implements SyncNativeAuth {
    private SyncSessionClient sessionClient;

    SyncNativeAuthClient(OIDCConfig mOIDCConfig, OktaState mOktaState, HttpConnectionFactory mConnectionFactory) {
        super(mOIDCConfig, mOktaState, mConnectionFactory);
        this.sessionClient = new SyncSessionClient(mOIDCConfig, mOktaState, mConnectionFactory);
    }

    @VisibleForTesting
    NativeAuthorizeRequest nativeAuthorizeRequest(String sessionToken,
                                                         AuthenticationPayload payload) {
        return new AuthorizeRequest.Builder()
                .account(mOIDCConfig)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .sessionToken(sessionToken)
                .authenticationPayload(payload)
                .createNativeRequest(mConnectionFactory);
    }

    @WorkerThread
    public AuthorizationResult logIn(String sessionToken,
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

    @Override
    public SyncSession getSessionClient() {
        return this.sessionClient;
    }
}
