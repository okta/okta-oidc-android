package com.okta.oidc.clients;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

public class SyncNativeAuthClientFactory extends AuthClientFactory<SyncNativeAuth> {
    @Override
    public SyncNativeAuthClient createClient(OIDCConfig mOIDCConfig, OktaState mOktaState, HttpConnectionFactory mConnectionFactory) {
        return new SyncNativeAuthClient(mOIDCConfig, mOktaState, mConnectionFactory);
    }
}
