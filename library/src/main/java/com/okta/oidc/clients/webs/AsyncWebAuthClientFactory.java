package com.okta.oidc.clients.webs;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;

import java.util.concurrent.Executor;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class AsyncWebAuthClientFactory extends AuthClientFactory<AsyncWebAuth> {
    private Executor mCallbackExecutor;
    private @ColorInt int mCustomTabColor;
    private String[] mSupportedBrowser;

    public AsyncWebAuthClientFactory(@Nullable Executor callbackExecutor, @ColorInt int mCustomTabColor, @Nullable String[] mSupportedBrowser) {
        this.mCallbackExecutor = callbackExecutor;
        this.mCustomTabColor = mCustomTabColor;
        this.mSupportedBrowser = mSupportedBrowser;
    }

    @Override
    public AsyncWebAuth createClient(OIDCAccount mOIDCAccount, OktaState mOktaState, HttpConnectionFactory mConnectionFactory) {
        return new AsyncWebAuthClient(mCallbackExecutor, mOIDCAccount, mOktaState, mConnectionFactory, mSupportedBrowser, mCustomTabColor);
    }
}
