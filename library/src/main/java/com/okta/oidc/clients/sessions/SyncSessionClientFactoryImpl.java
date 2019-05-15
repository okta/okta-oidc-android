package com.okta.oidc.clients.sessions;

import androidx.annotation.RestrictTo;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SyncSessionClientFactoryImpl {
    public SyncSessionClient createClient(OIDCConfig oidcConfig, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        return new SyncSessionClientImpl(oidcConfig, oktaState, connectionFactory);
    }
}
