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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.State;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.clients.sessions.SyncSessionClientImpl;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

class SyncAuthClientImpl extends AuthAPI implements SyncAuthClient {
    private SyncSessionClientImpl sessionClient;

    SyncAuthClientImpl(OIDCConfig mOIDCConfig, OktaState mOktaState,
                       HttpConnectionFactory mConnectionFactory) {
        super(mOIDCConfig, mOktaState, mConnectionFactory);
        sessionClient = new SyncSessionClientImpl(mOIDCConfig, mOktaState, mConnectionFactory);
    }

    @VisibleForTesting
    NativeAuthorizeRequest nativeAuthorizeRequest(String sessionToken,
                                                  AuthenticationPayload payload) {
        return new AuthorizeRequest.Builder()
                .config(mOIDCConfig)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .sessionToken(sessionToken)
                .authenticationPayload(payload)
                .createNativeRequest(mConnectionFactory);
    }

    @WorkerThread
    public AuthorizationResult logIn(String sessionToken,
                                     @Nullable AuthenticationPayload payload) {
        try {
            obtainNewConfiguration();
            mOktaState.setCurrentState(State.SIGN_IN_REQUEST);
            NativeAuthorizeRequest request = nativeAuthorizeRequest(sessionToken, payload);
            //FIXME Need to the parameters of native request in a web request because
            //oktaState uses it to verify the returned response.
            AuthorizeRequest authRequest = new AuthorizeRequest(request.getParameters());
            mOktaState.save(authRequest);
            AuthorizeResponse authResponse = request.executeRequest();
            validateResult(authResponse);
            mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
            TokenResponse tokenResponse = tokenExchange(authResponse).executeRequest();
            mOktaState.save(tokenResponse);
            return AuthorizationResult.success(new Tokens(tokenResponse));
        } catch (AuthorizationException e) {
            return AuthorizationResult.error(e);
        } finally {
            resetCurrentState();
        }
    }

    @Override
    public SyncSessionClient getSessionClient() {
        return this.sessionClient;
    }
}
