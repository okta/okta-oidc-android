package com.okta.oidc.clients.sessions;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.ClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;

public class SyncSessionClientFactory implements ClientFactory<SyncSessionClient> {
    @Override
    public SyncSessionClient createClient(OIDCConfig oidcConfig, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        return new SyncSessionClientImpl(oidcConfig, oktaState, connectionFactory);
    }
}
