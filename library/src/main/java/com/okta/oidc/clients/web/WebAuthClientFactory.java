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

package com.okta.oidc.clients.web;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.okta.oidc.CustomTabOptions;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.clients.ClientFactory;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;

import java.util.concurrent.Executor;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WebAuthClientFactory implements ClientFactory<WebAuthClient> {
    private Executor mCallbackExecutor;
    private CustomTabOptions mCustomTabOptions;
    private String[] mSupportedBrowser;

    public WebAuthClientFactory(@Nullable Executor callbackExecutor,
                                CustomTabOptions customTabOptions,
                                @Nullable String... supportedBrowser) {
        mCallbackExecutor = callbackExecutor;
        mCustomTabOptions = customTabOptions;
        mSupportedBrowser = supportedBrowser;
    }

    @Override
    public WebAuthClient createClient(OIDCConfig oidcConfig,
                                      Context context,
                                      OktaStorage oktaStorage,
                                      EncryptionManager encryptionManager,
                                      OktaHttpClient httpClient,
                                      boolean requireHardwareBackedKeyStore,
                                      boolean cacheMode) {
        return new WebAuthClientImpl(mCallbackExecutor, oidcConfig, context, oktaStorage,
                encryptionManager, httpClient, requireHardwareBackedKeyStore, cacheMode,
                mCustomTabOptions, mSupportedBrowser);
    }
}
