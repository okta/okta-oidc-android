package com.okta.oidc.sessions;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

public abstract class SessionClientFactory<S> {
    public abstract S createSession(OIDCAccount oidcAccount, OktaState oktaState, HttpConnectionFactory connectionFactory);
}