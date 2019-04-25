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
    /**
     * The Async Web Builder.
     */
    public static class AsyncWebBuilder extends OktaBuilder<AsyncWebAuth, AsyncWebBuilder> {
        private Executor mCallbackExecutor;
        private int mCustomTabColor;
        private String[] mSupportedBrowsers;

        /**
         * Sets a executor for use for callbacks. Default behaviour will execute
         * callbacks on the UI thread.
         *
         * @param executor custom executor
         * @return current builder
         */
        public AsyncWebBuilder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        /**
         * Sets the color for custom tab.
         *
         * @param customTabColor the custom tab color for the browser
         * @return current builder
         */
        public AsyncWebBuilder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        /**
         * Sets the supported browsers to use. The default is Chrome. Can use other
         * custom tab enabled browsers.
         *
         * @param browsers the package name of the browsers.
         * @return current builder
         */
        public AsyncWebBuilder supportedBrowsers(String[] browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        @Override
        AsyncWebBuilder toThis() {
            return this;
        }

        /**
         * Create AsyncWebAuth client.
         *
         * @return the authenticate client {@link AsyncWebAuth}
         */
        @Override
        public AsyncWebAuth create() {
            super.withAuthenticationClientFactory(new AsyncWebAuthClientFactory(this.mCallbackExecutor, mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }

    /**
     * The Sync Web Builder.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static class SyncWebBuilder extends OktaBuilder<SyncWebAuth, SyncWebBuilder> {
        private int mCustomTabColor;
        private String[] mSupportedBrowsers;

        /**
         * Sets the color for custom tab.
         *
         * @param customTabColor the custom tab color for the browser
         * @return current builder
         */
        public SyncWebBuilder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        /**
         * Sets the supported browsers to use. The default is Chrome. Can use other
         * custom tab enabled browsers.
         *
         * @param browsers the package name of the browsers.
         * @return current builder
         */
        public SyncWebBuilder supportedBrowsers(String[] browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        @Override
        SyncWebBuilder toThis() {
            return this;
        }

        /**
         * Create SyncWebAuth client.
         *
         * @return the authenticate client {@link SyncWebAuth}
         */
        @Override
        public SyncWebAuth create() {
            super.withAuthenticationClientFactory(new SyncWebAuthClientFactory(mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }

    /**
     * The Async Native Builder.
     */
    public static class AsyncNativeBuilder extends OktaBuilder<AsyncNativeAuth, AsyncNativeBuilder> {
        private Executor mCallbackExecutor;

        /**
         * Sets a executor for use for callbacks. Default behaviour will execute
         * callbacks on the UI thread.
         *
         * @param executor custom executor
         * @return current builder
         */
        public AsyncNativeBuilder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return toThis();
        }

        @Override
        AsyncNativeBuilder toThis() {
            return this;
        }

        /**
         * Create AsyncNativeAuth client.
         *
         * @return the authenticate client {@link AsyncNativeAuth}
         */
        @Override
        public AsyncNativeAuth create() {
            super.withAuthenticationClientFactory(new AsyncNativeAuthClientFactory(this.mCallbackExecutor));
            return createAuthClient();
        }
    }

    /**
     * The Sync Native Builder.
     */
    public static class SyncNativeBuilder extends OktaBuilder<SyncNativeAuth, SyncNativeBuilder> {

        @Override
        SyncNativeBuilder toThis() {
            return this;
        }

        /**
         * Create SyncNativeAuth client.
         *
         * @return the authenticate client {@link SyncNativeAuth}
         */
        @Override
        public SyncNativeAuth create() {
            super.withAuthenticationClientFactory(new SyncNativeAuthClientFactory());
            return createAuthClient();
        }
    }

    /**
     * The Custom Builder.
     */
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
