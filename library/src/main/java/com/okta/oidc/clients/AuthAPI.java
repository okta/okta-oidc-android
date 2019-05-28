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

package com.okta.oidc.clients;

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import static com.okta.oidc.clients.State.IDLE;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW;

/**
 * @hide
 *
 * This is a helper for authentication. It contains the APIs for getting
 * the provider configuration, validating results, and code exchange.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthAPI {
    protected OktaState mOktaState;
    protected OIDCConfig mOidcConfig;
    HttpConnectionFactory mConnectionFactory;

    protected AuthAPI(OIDCConfig oidcConfig,
                      Context context,
                      OktaStorage oktaStorage,
                      EncryptionManager encryptionManager,
                      HttpConnectionFactory connectionFactory,
                      boolean requireHardwareBackedKeyStore,
                      boolean cacheMode) {
        mOktaState = new OktaState(new OktaRepository(oktaStorage, context, encryptionManager, requireHardwareBackedKeyStore, cacheMode));
        mOidcConfig = oidcConfig;
        mConnectionFactory = connectionFactory;
    }

    protected ProviderConfiguration obtainNewConfiguration() throws AuthorizationException {
        try {
            ProviderConfiguration config = mOktaState.getProviderConfiguration();
            if (config == null || !mOidcConfig.getDiscoveryUri().toString().contains(config.issuer)) {
                mOktaState.setCurrentState(State.OBTAIN_CONFIGURATION);
                config = configurationRequest().executeRequest();
                mOktaState.save(config);
            }
            return config;
        } catch (OktaRepository.PersistenceException ex) {
            throw AuthorizationException.PersistenceErrors.byPersistenceException(ex);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public ConfigurationRequest configurationRequest() throws AuthorizationException {
        return HttpRequestBuilder.newConfigurationRequest()
                .connectionFactory(mConnectionFactory)
                .config(mOidcConfig)
                .createRequest();
    }

    protected void validateResult(WebResponse authResponse, WebRequest authorizedRequest) throws AuthorizationException {
        if (authorizedRequest == null) {
            throw USER_CANCELED_AUTH_FLOW;
        }

        String requestState = authorizedRequest.getState();
        String responseState = authResponse.getState();
        if (requestState == null && responseState != null
                || (requestState != null && !requestState
                .equals(responseState))) {
            throw AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH;
        }
    }

    @WorkerThread
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public TokenRequest tokenExchange(AuthorizeResponse response, ProviderConfiguration configuration, AuthorizeRequest authorizeRequest) throws AuthorizationException {
        return HttpRequestBuilder.newTokenRequest()
                .providerConfiguration(configuration)
                .config(mOidcConfig)
                .authRequest(authorizeRequest)
                .authResponse(response)
                .createRequest();
    }

    protected void resetCurrentState() {
        mOktaState.setCurrentState(IDLE);
    }

    @VisibleForTesting
    public OktaState getOktaState() {
        return mOktaState;
    }
}
