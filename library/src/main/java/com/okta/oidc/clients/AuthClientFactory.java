package com.okta.oidc.clients;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

public abstract class AuthClientFactory<A> {
    public abstract A createClient(OIDCAccount mOIDCAccount,
                                   OktaState mOktaState,
                                   HttpConnectionFactory mConnectionFactory);
}
