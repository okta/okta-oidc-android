/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.okta.oidc;

import androidx.annotation.ColorInt;

import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.clients.AuthClientFactoryImpl;
import com.okta.oidc.clients.SyncAuthClient;
import com.okta.oidc.clients.web.SyncWebAuthClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.clients.web.WebAuthClientFactory;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.clients.SyncAuthClientFactoryImpl;
import com.okta.oidc.clients.web.SyncWebAuthClientFactory;

import java.util.concurrent.Executor;

public class Okta {
    /**
     * The Async Web Builder.
     */
    public static class AsyncWebBuilder extends OktaBuilder<WebAuthClient, AsyncWebBuilder> {
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
         * Create WebAuthClient client.
         *
         * @return the authenticate client {@link WebAuthClient}
         */
        @Override
        public WebAuthClient create() {
            super.withAuthenticationClientFactory(new WebAuthClientFactory(this.mCallbackExecutor, mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }

    /**
     * The Sync Web Builder.
     */
    public static class SyncWebBuilder extends OktaBuilder<SyncWebAuthClient, SyncWebBuilder> {
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
        public SyncWebBuilder supportedBrowsers(String... browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        @Override
        SyncWebBuilder toThis() {
            return this;
        }

        /**
         * Create SyncWebAuthClient client.
         *
         * @return the authenticate client {@link SyncWebAuthClient}
         */
        @Override
        public SyncWebAuthClient create() {
            super.withAuthenticationClientFactory(new SyncWebAuthClientFactory(mCustomTabColor, mSupportedBrowsers));
            return createAuthClient();
        }
    }

    /**
     * The Async Native Builder.
     */
    public static class AsyncNativeBuilder extends OktaBuilder<AuthClient, AsyncNativeBuilder> {
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
         * @return the authenticate client {@link AuthClient}
         */
        @Override
        public AuthClient create() {
            super.withAuthenticationClientFactory(new AuthClientFactoryImpl(this.mCallbackExecutor));
            return createAuthClient();
        }
    }

    /**
     * The Sync Native Builder.
     */
    public static class SyncNativeBuilder extends OktaBuilder<SyncAuthClient, SyncNativeBuilder> {

        @Override
        SyncNativeBuilder toThis() {
            return this;
        }

        /**
         * Create SyncAuthClient client.
         *
         * @return the authenticate client {@link SyncAuthClient}
         */
        @Override
        public SyncAuthClient create() {
            super.withAuthenticationClientFactory(new SyncAuthClientFactoryImpl());
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
