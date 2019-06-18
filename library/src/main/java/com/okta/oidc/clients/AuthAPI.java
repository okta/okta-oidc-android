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
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.request.BaseRequest;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.clients.State.IDLE;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW;

/**
 * @hide This is a helper for authentication. It contains the APIs for getting
 * the provider configuration, validating results, and code exchange.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthAPI {
    protected OktaState mOktaState;
    protected OIDCConfig mOidcConfig;
    protected OktaHttpClient mHttpClient;

    protected AtomicBoolean mCancel = new AtomicBoolean();
    protected AtomicReference<WeakReference<BaseRequest>> mCurrentRequest =
            new AtomicReference<>(new WeakReference<>(null));

    protected AuthAPI(OIDCConfig oidcConfig,
                      Context context,
                      OktaStorage oktaStorage,
                      EncryptionManager encryptionManager,
                      boolean requireHardwareBackedKeyStore,
                      boolean cacheMode) {
        mOktaState = new OktaState(new OktaRepository(oktaStorage, context, encryptionManager,
                requireHardwareBackedKeyStore, cacheMode));
        mOidcConfig = oidcConfig;
    }

    protected ProviderConfiguration obtainNewConfiguration() throws AuthorizationException {
        try {
            ProviderConfiguration config = mOktaState.getProviderConfiguration();
            if (config == null ||
                    !mOidcConfig.getDiscoveryUri().toString().contains(config.issuer)) {
                mOktaState.setCurrentState(State.OBTAIN_CONFIGURATION);
                ConfigurationRequest request = configurationRequest();
                mCurrentRequest.set(new WeakReference<>(request));
                config = request.executeRequest(mHttpClient);
                mOktaState.save(config);
            }
            return config;
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public ConfigurationRequest configurationRequest() throws AuthorizationException {
        return HttpRequestBuilder.newConfigurationRequest()
                .config(mOidcConfig)
                .createRequest();
    }

    protected void validateResult(WebResponse authResponse, WebRequest authorizedRequest)
            throws AuthorizationException {
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
    public TokenRequest tokenExchange(AuthorizeResponse response,
                                      ProviderConfiguration configuration,
                                      AuthorizeRequest authorizeRequest)
            throws AuthorizationException {
        return HttpRequestBuilder.newTokenRequest()
                .providerConfiguration(configuration)
                .config(mOidcConfig)
                .authRequest(authorizeRequest)
                .authResponse(response)
                .createRequest();
    }

    protected void resetCurrentState() {
        mCancel.set(false);
        mOktaState.setCurrentState(IDLE);
    }

    @VisibleForTesting
    public OktaState getOktaState() {
        return mOktaState;
    }

    public void cancel() {
        mCancel.set(true);
        mHttpClient.cancel();
        if (mCurrentRequest.get().get() != null) {
            mCurrentRequest.get().get().cancelRequest();
        }
    }

    /*
     * Since sign-in is a collection of network ops. This method is used to check after each
     * network call if cancel was called. So even if the previous network call was successful
     * this check will stop further progress.
     */
    protected void checkIfCanceled() throws IOException {
        if (mCancel.get()) {
            throw new IOException("Canceled");
        }
    }
}
