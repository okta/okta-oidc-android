package com.okta.oidc.clients;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

public abstract class AuthClientFactory<A> {
    public abstract A createClient(OIDCConfig mOIDCConfig,
                                   OktaState mOktaState,
                                   HttpConnectionFactory mConnectionFactory);
}
