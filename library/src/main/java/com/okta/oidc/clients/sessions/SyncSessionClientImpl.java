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

package com.okta.oidc.clients.sessions;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.BaseRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.clients.State.IDLE;
import static com.okta.oidc.net.ConnectionParameters.DEFAULT_ENCODING;
import static com.okta.oidc.storage.OktaRepository.EncryptionException.INVALID_KEYS_ERROR;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR;

class SyncSessionClientImpl implements SyncSessionClient {
    private OIDCConfig mOidcConfig;
    private OktaState mOktaState;
    OktaHttpClient mHttpClient;
    private AtomicReference<WeakReference<BaseRequest>> mCurrentRequest =
            new AtomicReference<>(new WeakReference<>(null));

    SyncSessionClientImpl(OIDCConfig oidcConfig, OktaState oktaState,
                          OktaHttpClient httpClient) {
        mOidcConfig = oidcConfig;
        mOktaState = oktaState;
        mHttpClient = httpClient;
    }

    AuthorizedRequest createAuthorizedRequest(@NonNull Uri uri,
                                              @Nullable Map<String, String> properties,
                                              @Nullable Map<String, String> postParameters,
                                              @Nullable byte[] requestBody,
                                              @NonNull ConnectionParameters.RequestMethod method,
                                              ProviderConfiguration providerConfiguration,
                                              TokenResponse tokenResponse)
            throws AuthorizationException {
        return HttpRequestBuilder.newAuthorizedRequest()
                .config(mOidcConfig)
                .httpRequestMethod(method)
                .providerConfiguration(providerConfiguration)
                .tokenResponse(tokenResponse)
                .uri(uri)
                .properties(properties)
                .requestBody(requestBody)
                .postParameters(postParameters)
                .createRequest();
    }

    public JSONObject authorizedRequest(@NonNull Uri uri,
                                        @Nullable Map<String, String> properties,
                                        @Nullable Map<String, String> postParameters,
                                        @NonNull ConnectionParameters.RequestMethod method)
            throws AuthorizationException {
        try {
            ProviderConfiguration providerConfiguration = mOktaState.getProviderConfiguration();
            TokenResponse tokenResponse = mOktaState.getTokenResponse();
            AuthorizedRequest request = createAuthorizedRequest(uri, properties, postParameters,
                    null, method, providerConfiguration, tokenResponse);
            mCurrentRequest.set(new WeakReference<>(request));
            ByteBuffer buffer = request.executeRequest(mHttpClient);
            String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
            return new JSONObject(json);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        } catch (JSONException e) {
            throw AuthorizationException.fromTemplate(JSON_DESERIALIZATION_ERROR, e);
        }
    }

    public ByteBuffer authorizedRequest(@NonNull Uri uri,
                                        @Nullable Map<String, String> properties,
                                        @Nullable Map<String, String> postParameters,
                                        @Nullable byte[] requestBody,
                                        @NonNull ConnectionParameters.RequestMethod method)
            throws AuthorizationException {
        try {
            ProviderConfiguration providerConfiguration = mOktaState.getProviderConfiguration();
            TokenResponse tokenResponse = mOktaState.getTokenResponse();
            AuthorizedRequest request = createAuthorizedRequest(uri, properties, null,
                    requestBody, method, providerConfiguration, tokenResponse);
            mCurrentRequest.set(new WeakReference<>(request));

            return request.executeRequest(mHttpClient);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    AuthorizedRequest userProfileRequest(ProviderConfiguration providerConfiguration,
                                         TokenResponse tokenResponse)
            throws AuthorizationException {
        if (mOidcConfig.isOAuth2Configuration()) {
            throw new AuthorizationException("Invalid operation. " +
                    "Please check your configuration. OAuth2 authorization servers does not" +
                    "support /userinfo endpoint ", new RuntimeException());
        }
        return HttpRequestBuilder.newProfileRequest()
                .tokenResponse(tokenResponse)
                .providerConfiguration(providerConfiguration)
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public UserInfo getUserProfile() throws AuthorizationException {
        try {
            ProviderConfiguration providerConfiguration = mOktaState.getProviderConfiguration();
            TokenResponse tokenResponse = mOktaState.getTokenResponse();
            AuthorizedRequest request = userProfileRequest(providerConfiguration, tokenResponse);
            ByteBuffer buffer = request.executeRequest(mHttpClient);
            String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
            JSONObject userInfo = new JSONObject(json);
            mCurrentRequest.set(new WeakReference<>(request));
            return new UserInfo(userInfo);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        } catch (JSONException e) {
            throw AuthorizationException.fromTemplate(JSON_DESERIALIZATION_ERROR, e);
        }
    }

    IntrospectRequest introspectTokenRequest(String token, String tokenType,
                                             ProviderConfiguration providerConfiguration)
            throws AuthorizationException {
        return HttpRequestBuilder.newIntrospectRequest()
                .introspect(token, tokenType)
                .providerConfiguration(providerConfiguration)
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public IntrospectInfo introspectToken(String token, String tokenType)
            throws AuthorizationException {
        try {
            IntrospectRequest request = introspectTokenRequest(token, tokenType,
                    mOktaState.getProviderConfiguration());
            mCurrentRequest.set(new WeakReference<>(request));
            return request.executeRequest(mHttpClient);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    RevokeTokenRequest revokeTokenRequest(String token, ProviderConfiguration providerConfiguration)
            throws AuthorizationException {
        return HttpRequestBuilder.newRevokeTokenRequest()
                .tokenToRevoke(token)
                .providerConfiguration(providerConfiguration)
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public Boolean revokeToken(String token) throws AuthorizationException {
        try {
            RevokeTokenRequest request = revokeTokenRequest(token,
                    mOktaState.getProviderConfiguration());
            mCurrentRequest.set(new WeakReference<>(request));
            return request.executeRequest(mHttpClient);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    RefreshTokenRequest refreshTokenRequest(ProviderConfiguration providerConfiguration,
                                            TokenResponse tokenResponse)
            throws AuthorizationException {
        return HttpRequestBuilder.newRefreshTokenRequest()
                .tokenResponse(tokenResponse)
                .providerConfiguration(providerConfiguration)
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public Tokens refreshToken() throws AuthorizationException {
        try {
            RefreshTokenRequest request = refreshTokenRequest(mOktaState.getProviderConfiguration(),
                    mOktaState.getTokenResponse());
            mCurrentRequest.set(new WeakReference<>(request));
            TokenResponse tokenResponse = request.executeRequest(mHttpClient);
            mOktaState.save(tokenResponse);
            return new Tokens(tokenResponse);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    @Override
    public Tokens getTokens() throws AuthorizationException {
        try {
            TokenResponse response = mOktaState.getTokenResponse();
            if (response == null) {
                return null;
            }
            return new Tokens(response);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    @Override
    public boolean isAuthenticated() {
        boolean hasTokenResponse = mOktaState.hasTokenResponse();
        if (!hasTokenResponse) {
            return false;
        }
        try {
            TokenResponse tokenResponse = mOktaState.getTokenResponse();
            return tokenResponse != null;
        } catch (OktaRepository.EncryptionException e) {
            // Here we check if we can decrypt saved token.
            // For Android API 18-22, there is some problem to determinate if private keys are valid
            // If user set Screen Lock in Android Settings, all created before keys removed,
            // and new one will create, when we initialize EncryptionManager.
            // So we haven't way to check if we can decrypt persisted data.
            // For Android API 23+, possible another EncryptionException error, so it's mean that
            // keys are valid but require additional authentication by user.
            if (e.getType() == INVALID_KEYS_ERROR) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        mOktaState.delete(ProviderConfiguration.RESTORE.getKey());
        mOktaState.delete(TokenResponse.RESTORE.getKey());
        mOktaState.delete(WebRequest.RESTORE.getKey());
        mOktaState.setCurrentState(IDLE);
    }

    @Override
    public void cancel() {
        mHttpClient.cancel();
        if (mCurrentRequest.get().get() != null) {
            mCurrentRequest.get().get().cancelRequest();
        }
    }

    @Override
    public void migrateTo(EncryptionManager manager) throws AuthorizationException {
        try {
            final ProviderConfiguration providerConfiguration =
                    mOktaState.getProviderConfiguration();
            final TokenResponse tokenResponse = mOktaState.getTokenResponse();
            final WebRequest authorizedRequest = mOktaState.getAuthorizeRequest();

            clear();

            mOktaState.setEncryptionManager(manager);

            mOktaState.save(providerConfiguration);
            mOktaState.save(tokenResponse);
            mOktaState.save(authorizedRequest);
        } catch (OktaRepository.EncryptionException e) {
            throw AuthorizationException.EncryptionErrors.byEncryptionException(e);
        }
    }

    OktaState getOktaState() {
        return mOktaState;
    }
}
