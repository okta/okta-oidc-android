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

import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.AnimRes;
import androidx.annotation.ColorInt;

import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.clients.AuthClientFactoryImpl;
import com.okta.oidc.clients.SyncAuthClient;
import com.okta.oidc.clients.SyncAuthClientFactory;
import com.okta.oidc.clients.web.SyncWebAuthClient;
import com.okta.oidc.clients.web.SyncWebAuthClientFactory;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.clients.web.WebAuthClientFactory;

import java.util.concurrent.Executor;

/**
 * A collection of builders for creating different type of authentication clients.
 * {@link WebAuthClient}
 * {@link AuthClient}
 * {@link SyncAuthClient}
 */
public class Okta {
    /**
     * The asynchronous web authentication client builder.
     */
    public static class WebAuthBuilder extends OktaBuilder<WebAuthClient, WebAuthBuilder> {
        private Executor mCallbackExecutor;
        private final CustomTabOptions customTabOptions = new CustomTabOptions();
        private String[] mSupportedBrowsers;

        /**
         * Sets a executor for use for callbacks. Default behaviour will execute
         * callbacks on the UI thread.
         *
         * @param executor custom executor
         * @return current builder
         */
        public WebAuthBuilder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        /**
         * Sets the color for custom tab.
         *
         * @param customTabColor the custom tab color for the browser
         * @return current builder
         */
        public WebAuthBuilder withTabColor(@ColorInt int customTabColor) {
            customTabOptions.setCustomTabColor(customTabColor);
            return this;
        }

        /**
         * Sets entrance animations when transitioning to chrome custom tab.
         *
         * @param enterResId Resource ID of the "enter" animation for the browser.
         * @param exitResId  Resource ID of the "exit" animation for the application.
         * @return current builder
         */
        public WebAuthBuilder withStartAnimation(@AnimRes int enterResId, @AnimRes int exitResId) {
            customTabOptions.setStartEnterResId(enterResId);
            customTabOptions.setStartExitResId(exitResId);
            return this;
        }

        /**
         * Sets exit animations when transitioning out of chrome custom tab.
         *
         * @param enterResId Resource ID of the "enter" animation for the application.
         * @param exitResId  Resource ID of the "exit" animation for the browser.
         * @return current builder
         */
        public WebAuthBuilder withExitAnimation(@AnimRes int enterResId, @AnimRes int exitResId) {
            customTabOptions.setEndEnterResId(enterResId);
            customTabOptions.setEndExitResId(exitResId);
            return this;
        }

        /**
         * Sets the supported browsers to use. The default is Chrome. Can use other
         * custom tab enabled browsers.
         *
         * @param browsers the package name of the browsers.
         * @return current builder
         */
        public WebAuthBuilder supportedBrowsers(String... browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        /**
         * Sets the supported browsers query to use the MATCH_ALL intent flag.
         *
         * @param matchAll set true to use {@link android.content.pm.PackageManager#MATCH_ALL}
         * @return current builder
         */
        public WebAuthBuilder browserMatchAll(boolean matchAll) {
            int flag = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && matchAll) {
                flag = PackageManager.MATCH_ALL;
            }
            customTabOptions.setBrowserMatchAllFlag(flag);
            return this;
        }

        @Override
        protected WebAuthBuilder toThis() {
            return this;
        }

        /**
         * Create WebAuthClient client.
         *
         * @return the authenticate client {@link WebAuthClient}
         */
        @Override
        public WebAuthClient create() {
            super.withAuthenticationClientFactory(new WebAuthClientFactory(mCallbackExecutor,
                    customTabOptions, mSupportedBrowsers));
            return createAuthClient();
        }
    }

    /**
     * The synchronous web authentication client builder.
     */
    public static class SyncWebAuthBuilder extends OktaBuilder<SyncWebAuthClient, SyncWebAuthBuilder> {
        private final CustomTabOptions customTabOptions = new CustomTabOptions();
        private String[] mSupportedBrowsers;

        /**
         * Sets the color for custom tab.
         *
         * @param customTabColor the custom tab color for the browser
         * @return current builder
         */
        public SyncWebAuthBuilder withTabColor(@ColorInt int customTabColor) {
            customTabOptions.setCustomTabColor(customTabColor);
            return this;
        }

        /**
         * Sets entrance animations when transitioning to chrome custom tab.
         *
         * @param enterResId Resource ID of the "enter" animation for the browser.
         * @param exitResId  Resource ID of the "exit" animation for the application.
         * @return current builder
         */
        public SyncWebAuthBuilder withStartAnimation(@AnimRes int enterResId,
                                                     @AnimRes int exitResId) {
            customTabOptions.setStartEnterResId(enterResId);
            customTabOptions.setStartExitResId(exitResId);
            return this;
        }

        /**
         * Sets exit animations when transitioning out of chrome custom tab.
         *
         * @param enterResId Resource ID of the "enter" animation for the application.
         * @param exitResId  Resource ID of the "exit" animation for the browser.
         * @return current builder
         */
        public SyncWebAuthBuilder withExitAnimation(@AnimRes int enterResId,
                                                    @AnimRes int exitResId) {
            customTabOptions.setEndEnterResId(enterResId);
            customTabOptions.setEndExitResId(exitResId);
            return this;
        }

        /**
         * Sets the supported browsers to use. The default is Chrome. Can use other
         * custom tab enabled browsers.
         *
         * @param browsers the package name of the browsers.
         * @return current builder
         */
        public SyncWebAuthBuilder supportedBrowsers(String... browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }

        /**
         * Sets the supported browsers query to use the MATCH_ALL intent flag.
         *
         * @param matchAll set true to use {@link android.content.pm.PackageManager#MATCH_ALL}
         * @return current builder
         */
        public SyncWebAuthBuilder browserMatchAll(boolean matchAll) {
            int flag = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && matchAll) {
                flag = PackageManager.MATCH_ALL;
            }
            customTabOptions.setBrowserMatchAllFlag(flag);
            return this;
        }

        @Override
        protected SyncWebAuthBuilder toThis() {
            return this;
        }

        /**
         * Create SyncWebAuthClient client.
         *
         * @return the authenticate client {@link SyncWebAuthClient}
         */
        @Override
        public SyncWebAuthClient create() {
            super.withAuthenticationClientFactory(
                    new SyncWebAuthClientFactory(customTabOptions, mSupportedBrowsers));
            return createAuthClient();
        }
    }

    /**
     * The asynchronous authentication client builder using sessionTokens.
     */
    public static class AuthBuilder extends OktaBuilder<AuthClient, AuthBuilder> {
        private Executor mCallbackExecutor;

        /**
         * Sets a executor for use for callbacks. Default behaviour will execute
         * callbacks on the UI thread.
         *
         * @param executor custom executor
         * @return current builder
         */
        public AuthBuilder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return toThis();
        }

        @Override
        protected AuthBuilder toThis() {
            return this;
        }

        /**
         * Create AuthClient.
         *
         * @return the authenticate client {@link AuthClient}
         */
        @Override
        public AuthClient create() {
            super.withAuthenticationClientFactory(
                    new AuthClientFactoryImpl(this.mCallbackExecutor));
            return createAuthClient();
        }
    }

    /**
     * The synchronous authentication client builder using sessionTokens.
     */
    public static class SyncAuthBuilder extends OktaBuilder<SyncAuthClient, SyncAuthBuilder> {

        @Override
        protected SyncAuthBuilder toThis() {
            return this;
        }

        /**
         * Create SyncAuthClient.
         *
         * @return the authenticate client {@link SyncAuthClient}
         */
        @Override
        public SyncAuthClient create() {
            super.withAuthenticationClientFactory(new SyncAuthClientFactory());
            return createAuthClient();
        }
    }
}
