package com.okta.oidc;

import androidx.annotation.ColorInt;
import androidx.annotation.VisibleForTesting;

import com.okta.oidc.clients.AsyncNativeAuth;
import com.okta.oidc.clients.AsyncNativeAuthClientFactory;
import com.okta.oidc.clients.AsyncWebAuth;
import com.okta.oidc.clients.AsyncWebAuthClientFactory;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.clients.SyncNativeAuth;
import com.okta.oidc.clients.SyncNativeAuthClientFactory;
import com.okta.oidc.clients.SyncWebAuth;
import com.okta.oidc.clients.SyncWebAuthClientFactory;

import java.util.concurrent.Executor;

public class Okta {


    public static class AsyncWebBuilder extends OktaBuilder<AsyncWebAuth, AsyncWebBuilder> {
        private Executor mCallbackExecutor;
        private int mCustomTabColor;
        private String[] mSupportedBrowsers;

        public AsyncWebBuilder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        public AsyncWebBuilder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        public AsyncWebBuilder supportedBrowsers(String[] browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        @Override
        AsyncWebBuilder toThis() {
            return this;
        }

        @Override
        public AsyncWebAuth create() {
            super.withAuthenticationClientFactory(new AsyncWebAuthClientFactory(this.mCallbackExecutor, mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static class SyncWebBuilder extends OktaBuilder<SyncWebAuth, SyncWebBuilder> {
        private int mCustomTabColor;
        private String[] mSupportedBrowsers;

        public SyncWebBuilder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        public SyncWebBuilder supportedBrowsers(String[] browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        @Override
        SyncWebBuilder toThis() {
            return this;
        }

        @Override
        public SyncWebAuth create() {
            super.withAuthenticationClientFactory(new SyncWebAuthClientFactory(mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }
    public static class AsyncNativeBuilder extends OktaBuilder<AsyncNativeAuth, AsyncNativeBuilder> {
        private Executor mCallbackExecutor;

        public AsyncNativeBuilder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return toThis();
        }

        @Override
        AsyncNativeBuilder toThis() {
            return this;
        }

        @Override
        public AsyncNativeAuth create() {
            super.withAuthenticationClientFactory(new AsyncNativeAuthClientFactory(this.mCallbackExecutor));
            return createAuthClient();
        }
    }
    public static class SyncNativeBuilder extends OktaBuilder<SyncNativeAuth, SyncNativeBuilder> {

        @Override
        SyncNativeBuilder toThis() {
            return this;
        }

        @Override
        public SyncNativeAuth create() {
            super.withAuthenticationClientFactory(new SyncNativeAuthClientFactory());
            return createAuthClient();
        }
    }

    public static class Builder<A> extends OktaBuilder<A, Builder<A>> {
        public Builder() {
        }

        @Override
        public A create() {
            return createAuthClient();
        }


        @Override
        public Builder<A> withAuthenticationClientFactory(AuthClientFactory<A> authClientFactory) {
            return super.withAuthenticationClientFactory(authClientFactory);
        }

        @Override
        Builder<A> toThis() {
            return this;
        }
    }
}
