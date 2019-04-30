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

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.State;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.WebResponse;
import com.okta.oidc.util.AuthorizationException;

import static com.okta.oidc.State.IDLE;
import static com.okta.oidc.net.request.HttpRequest.Type.TOKEN_EXCHANGE;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW;

/**
 * This is a helper for authentication. It contains the APIs for getting
 * the provider configuration, validating results, and code exchange.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthAPI {
    protected OktaState mOktaState;
    protected OIDCConfig mOIDCConfig;
    HttpConnectionFactory mConnectionFactory;

    protected AuthAPI(OIDCConfig oidcConfig, OktaState oktaState,
                      HttpConnectionFactory connectionFactory) {
        mOktaState = oktaState;
        mOIDCConfig = oidcConfig;
        mConnectionFactory = connectionFactory;
    }

    protected void obtainNewConfiguration() throws AuthorizationException {
        ProviderConfiguration config = mOktaState.getProviderConfiguration();
        if (config == null || !mOIDCConfig.getDiscoveryUri().toString().contains(config.issuer)) {
            mOktaState.setCurrentState(State.OBTAIN_CONFIGURATION);
            mOktaState.save(configurationRequest().executeRequest());
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public ConfigurationRequest configurationRequest() {
        return (ConfigurationRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.CONFIGURATION)
                .connectionFactory(mConnectionFactory)
                .account(mOIDCConfig)
                .createRequest();
    }

    protected void validateResult(WebResponse authResponse) throws AuthorizationException {
        WebRequest authorizedRequest = mOktaState.getAuthorizeRequest();
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
    public TokenRequest tokenExchange(AuthorizeResponse response) {
        return (TokenRequest) HttpRequestBuilder.newRequest()
                .request(TOKEN_EXCHANGE)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCConfig)
                .authRequest((AuthorizeRequest) mOktaState.getAuthorizeRequest())
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
