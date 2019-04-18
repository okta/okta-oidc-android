package com.okta.oidc.factory.session;

public abstract class SessionClientFactory<S> {
    public abstract S createSession();
}