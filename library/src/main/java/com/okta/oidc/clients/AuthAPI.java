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
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SyncSessionClient;
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

import static androidx.annotation.RestrictTo.Scope.TESTS;

import static com.okta.oidc.clients.BaseAuth.FAILED_CLEAR_DATA;
import static com.okta.oidc.clients.BaseAuth.FAILED_REVOKE_ACCESS_TOKEN;
import static com.okta.oidc.clients.BaseAuth.FAILED_REVOKE_REFRESH_TOKEN;
import static com.okta.oidc.clients.BaseAuth.REMOVE_TOKENS;
import static com.okta.oidc.clients.BaseAuth.REVOKE_ACCESS_TOKEN;
import static com.okta.oidc.clients.BaseAuth.REVOKE_REFRESH_TOKEN;
import static com.okta.oidc.clients.BaseAuth.TOKEN_DECRYPT;
import static com.okta.oidc.clients.State.IDLE;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW;
import static com.okta.oidc.util.AuthorizationException.TYPE_ENCRYPTION_ERROR;

/**
 * @hide This is a helper for authentication. It contains the APIs for getting
 * the provider configuration, validating results, and code exchange.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthAPI {
    private static final String TAG = "AuthClientImpl";
    protected OktaState mOktaState;
    protected OIDCConfig mOidcConfig;
    protected OktaHttpClient mHttpClient;
    protected int mSignOutFlags;
    protected int mSignOutStatus;

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
            Uri discoveryUri = mOidcConfig.getDiscoveryUri();
            if (discoveryUri != null) {
                if (config == null || !discoveryUri.toString().contains(config.issuer)) {
                    mOktaState.setCurrentState(State.OBTAIN_CONFIGURATION);
                    ConfigurationRequest request = configurationRequest();
                    mCurrentRequest.set(new WeakReference<>(request));
                    config = request.executeRequest(mHttpClient);
                    mOktaState.save(config);
                }
            } else {
                config = new ProviderConfiguration(mOidcConfig.getCustomConfiguration());
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

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public boolean isVerificationFlow(AuthorizeResponse response)
            throws AuthorizationException {
        if (!TextUtils.isEmpty(response.getError())) {
            throw new AuthorizationException(response.getErrorDescription(), null);
        }
        String typeHint = response.getTypeHint();
        return !TextUtils.isEmpty(typeHint) &&
                typeHint.equals(AuthorizeResponse.ACTIVATION);
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

    protected void clear() {
        mOktaState.delete(ProviderConfiguration.RESTORE.getKey());
        mOktaState.delete(TokenResponse.RESTORE.getKey());
        mOktaState.delete(WebRequest.RESTORE.getKey());
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
     * Since sign-in/revokeTokens is a collection of network ops. This method is used to check
     * after each network call if cancel was called. So even if the previous network call was
     * successful this check will stop further progress.
     */
    protected void checkIfCanceled() throws IOException {
        if (mCancel.get()) {
            throw new IOException("Canceled");
        }
    }

    private int revoke(SyncSessionClient client, int tokenType) {
        try {
            Tokens tokens = client.getTokens();
            if (tokens != null) {
                client.revokeToken(tokenType == REVOKE_ACCESS_TOKEN ? tokens.getAccessToken()
                        : tokens.getRefreshToken());
            }
            return 0;
        } catch (AuthorizationException e) {
            Log.w(TAG, "Revoke token failure", e);
            int status = tokenType == REVOKE_ACCESS_TOKEN ? FAILED_REVOKE_ACCESS_TOKEN
                    : FAILED_REVOKE_REFRESH_TOKEN;
            if (e.type == TYPE_ENCRYPTION_ERROR) {
                status |= TOKEN_DECRYPT;
            }
            return status;
        }
    }

    protected void removeTokens(SyncSessionClient client) {
        if ((mSignOutFlags & REMOVE_TOKENS) == REMOVE_TOKENS) {
            if ((mSignOutStatus & FAILED_REVOKE_REFRESH_TOKEN) == 0 &&
                    (mSignOutStatus & FAILED_REVOKE_ACCESS_TOKEN) == 0) {
                client.clear();
            } else {
                mSignOutStatus |= FAILED_CLEAR_DATA;
            }
        }
    }

    protected void revokeTokens(SyncSessionClient client) throws IOException {
        if ((mSignOutFlags & REVOKE_ACCESS_TOKEN) == REVOKE_ACCESS_TOKEN) {
            mSignOutStatus |= revoke(client, REVOKE_ACCESS_TOKEN);
        }
        checkIfCanceled();

        if ((mSignOutFlags & REVOKE_REFRESH_TOKEN) == REVOKE_REFRESH_TOKEN) {
            mSignOutStatus |= revoke(client, REVOKE_REFRESH_TOKEN);
        }
        checkIfCanceled();
    }

    @RestrictTo(TESTS)
    public int getSignOutFlags() {
        return mSignOutFlags;
    }

    @RestrictTo(TESTS)
    public int getSignOutStatus() {
        return mSignOutStatus;
    }
}
