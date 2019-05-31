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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.clients.sessions.SyncSessionClientFactoryImpl;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import java.io.IOException;
import java.lang.ref.WeakReference;

class SyncAuthClientImpl extends AuthAPI implements SyncAuthClient {
    private SyncSessionClient sessionClient;

    SyncAuthClientImpl(OIDCConfig oidcConfig,
                       Context context,
                       OktaStorage oktaStorage,
                       EncryptionManager encryptionManager,
                       HttpConnectionFactory connectionFactory,
                       boolean requireHardwareBackedKeyStore,
                       boolean cacheMode) {
        super(oidcConfig, context, oktaStorage, encryptionManager, connectionFactory,
                requireHardwareBackedKeyStore, cacheMode);
        sessionClient = new SyncSessionClientFactoryImpl()
                .createClient(oidcConfig, mOktaState, connectionFactory);
    }

    @VisibleForTesting
    NativeAuthorizeRequest nativeAuthorizeRequest(String sessionToken,
                                                  ProviderConfiguration providerConfiguration,
                                                  AuthenticationPayload payload)
            throws AuthorizationException {
        return new AuthorizeRequest.Builder()
                .config(mOidcConfig)
                .providerConfiguration(providerConfiguration)
                .sessionToken(sessionToken)
                .authenticationPayload(payload)
                .createNativeRequest(mConnectionFactory);
    }

    @WorkerThread
    public Result signIn(String sessionToken,
                         @Nullable AuthenticationPayload payload) {
        try {
            mCancel.set(false);
            ProviderConfiguration providerConfiguration = obtainNewConfiguration();
            checkIfCanceled();

            mOktaState.setCurrentState(State.SIGN_IN_REQUEST);
            NativeAuthorizeRequest request = nativeAuthorizeRequest(sessionToken,
                    providerConfiguration, payload);
            mCurrentRequest.set(new WeakReference<>(request));

            //Save the nativeAuth request in a AuthRequest because it is needed to verify results.
            AuthorizeRequest authRequest = new AuthorizeRequest(request.getParameters());
            mOktaState.save(authRequest);
            AuthorizeResponse authResponse = request.executeRequest();
            checkIfCanceled();

            validateResult(authResponse, authRequest);
            mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
            TokenRequest requestToken = tokenExchange(authResponse, providerConfiguration,
                    authRequest);
            mCurrentRequest.set(new WeakReference<>(requestToken));
            TokenResponse tokenResponse = requestToken.executeRequest();

            mOktaState.save(tokenResponse);
            return Result.success();
        } catch (AuthorizationException e) {
            return Result.error(e);
        } catch (IOException e) {
            return Result.cancel();
        } catch (Exception e) {
            return Result.error(AuthorizationException.AuthorizationRequestErrors.OTHER);
        } finally {
            resetCurrentState();
        }
    }

    @Override
    public void migrateTo(EncryptionManager manager) throws AuthorizationException {
        getSessionClient().migrateTo(manager);
    }

    @Override
    public SyncSessionClient getSessionClient() {
        return this.sessionClient;
    }
}
