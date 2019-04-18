package com.okta.oidc.clients;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class SyncWebAuthClientFactory extends AuthClientFactory<SyncWebAuth> {
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

    public SyncWebAuthClientFactory(@ColorInt int customTabColor, @Nullable String[] mSupportedBrowser) {
        this.customTabColor = customTabColor;
        this.mSupportedBrowsers = mSupportedBrowsers;
    }

    @Override
    public SyncWebAuth createClient(OIDCConfig mOIDCConfig,
                                          OktaState mOktaState,
                                          HttpConnectionFactory mConnectionFactory) {
        return new SyncWebAuthClient(mOIDCConfig, mOktaState, mConnectionFactory, mSupportedBrowsers, customTabColor);
    }
}
