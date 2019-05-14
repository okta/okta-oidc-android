package com.okta.oidc.clients.sessions;

import androidx.annotation.RestrictTo;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

import java.util.concurrent.Executor;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SessionClientFactoryImpl implements SessionClientFactory<SessionClient> {
    private Executor executor;

    public SessionClientFactoryImpl(Executor executor) {
        this.executor = executor;
    }

    @Override
    public SessionClient createClient(OIDCConfig oidcConfig, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        return new SessionClientImpl(executor, oidcConfig, oktaState, connectionFactory);
    }
}
