package com.okta.oidc.factory.client.sync;

import com.okta.oidc.factory.client.AuthClientFactory;

public class SyncAuthClientFactory extends AuthClientFactory<BrowserSyncAuthClient, NativeSyncAuthClient> {

    @Override
    public BrowserSyncAuthClient createBrowser() {
        return new BrowserSyncAuthClient();
    }

    @Override
    public NativeSyncAuthClient createNative() {
        return new NativeSyncAuthClient();
    }
}
