package com.okta.oidc.factory.client.async;

import com.okta.oidc.factory.client.AuthClientFactory;

public class AsyncAuthClientFactory extends AuthClientFactory<BrowserAsyncAuthClient, NativeAsyncAuthClient> {
    @Override
    public BrowserAsyncAuthClient createBrowser() {
        return new BrowserAsyncAuthClient();
    }

    @Override
    public NativeAsyncAuthClient createNative() {
        return new NativeAsyncAuthClient();
    }
}
