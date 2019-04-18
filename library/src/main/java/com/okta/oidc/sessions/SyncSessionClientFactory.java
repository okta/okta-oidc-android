package com.okta.oidc.sessions;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

public class SyncSessionClientFactory extends SessionClientFactory<SyncSession> {
    @Override
    public SyncSession createSession(OIDCAccount oidcAccount, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        return new SyncSessionClient(oidcAccount, oktaState, connectionFactory);
    }

}
