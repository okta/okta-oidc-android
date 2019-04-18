package com.okta.oidc.factory.client;

public abstract class AuthClientFactory<B, N> {
    public abstract B createBrowser();
    public abstract N createNative();
}
