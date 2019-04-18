package com.okta.oidc.clients.webs;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class SyncWebAuthClientFactory extends AuthClientFactory<SyncWebAuthClient> {
    @ColorInt
    private int customTabColor;
    private String[] mSupportedBrowsers;

    public SyncWebAuthClientFactory() { }

    public SyncWebAuthClientFactory(int customTabColor) {
        this.customTabColor = customTabColor;
    }

    public SyncWebAuthClientFactory(String[] mSupportedBrowsers) {
        this.mSupportedBrowsers = mSupportedBrowsers;
    }

    public SyncWebAuthClientFactory(@ColorInt int customTabColor, @Nullable String... mSupportedBrowsers) {
        this.customTabColor = customTabColor;
        this.mSupportedBrowsers = mSupportedBrowsers;
    }

    @Override
    public SyncWebAuthClient createClient(OIDCAccount mOIDCAccount,
                                          OktaState mOktaState,
                                          HttpConnectionFactory mConnectionFactory) {
        return new SyncWebAuthClient(mOIDCAccount, mOktaState, mConnectionFactory, mSupportedBrowsers, customTabColor);
    }
}
