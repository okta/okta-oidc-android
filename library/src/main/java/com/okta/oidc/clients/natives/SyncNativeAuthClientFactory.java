package com.okta.oidc.clients.natives;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;

public class SyncNativeAuthClientFactory extends AuthClientFactory<SyncNativeAuthClient> {
    @Override
    public SyncNativeAuthClient createClient(OIDCAccount mOIDCAccount, OktaState mOktaState, HttpConnectionFactory mConnectionFactory) {
        return new SyncNativeAuthClient(mOIDCAccount, mOktaState, mConnectionFactory);
    }
}
