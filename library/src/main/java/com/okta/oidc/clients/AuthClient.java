package com.okta.oidc.clients;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.State;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.util.AuthorizationException;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import static com.okta.oidc.State.IDLE;
import static com.okta.oidc.net.request.HttpRequest.Type.TOKEN_EXCHANGE;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW;

public class AuthClient {
    protected OktaState mOktaState;
    protected OIDCConfig mOIDCConfig;
    protected HttpConnectionFactory mConnectionFactory;

    public AuthClient(OIDCConfig mOIDCConfig, OktaState mOktaState, HttpConnectionFactory connectionFactory) {
        this.mOktaState = mOktaState;
        this.mOIDCConfig = mOIDCConfig;
        this.mConnectionFactory = connectionFactory;
    }

    protected void obtainNewConfiguration() throws AuthorizationException {
        ProviderConfiguration config = mOktaState.getProviderConfiguration();
        if (config == null || !config.issuer.equals(mOIDCConfig.getDiscoveryUri())) {
            mOktaState.setCurrentState(State.OBTAIN_CONFIGURATION);
            mOktaState.save(configurationRequest().executeRequest());
        }
    }

    protected ConfigurationRequest configurationRequest() {
        return (ConfigurationRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.CONFIGURATION)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCConfig).createRequest();
    }

    protected void validateResult(WebResponse authResponse) throws AuthorizationException {
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
                .account(mOIDCConfig)
                .authRequest((AuthorizeRequest) mOktaState.getAuthorizeRequest())
                .authResponse(response)
                .createRequest();
    }

    protected void resetCurrentState() {
        mOktaState.setCurrentState(IDLE);
    }

    @VisibleForTesting
    OktaState getOktaState() {
        return mOktaState;
    }
}
