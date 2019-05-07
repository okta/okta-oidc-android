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
import androidx.annotation.RestrictTo;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;

import static com.okta.oidc.State.IDLE;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SyncSessionClientImpl implements SyncSessionClient {
    private OIDCConfig mOidcConfig;
    private OktaState mOktaState;
    private HttpConnectionFactory mConnectionFactory;

    public SyncSessionClientImpl(OIDCConfig oidcConfig, OktaState oktaState,
                                 HttpConnectionFactory connectionFactory) {
        mOidcConfig = oidcConfig;
        mOktaState = oktaState;
        mConnectionFactory = connectionFactory;
    }

    AuthorizedRequest createAuthorizedRequest(@NonNull Uri uri,
                                              @Nullable Map<String, String> properties,
                                              @Nullable Map<String, String> postParameters,
                                              @NonNull HttpConnection.RequestMethod method) {
        return (AuthorizedRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.AUTHORIZED)
                .connectionFactory(mConnectionFactory)
                .config(mOidcConfig)
                .httpRequestMethod(method)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .tokenResponse(mOktaState.getTokenResponse())
                .uri(uri)
                .properties(properties)
                .postParameters(postParameters)
                .createRequest();
    }

    public JSONObject authorizedRequest(@NonNull Uri uri,
                                        @Nullable Map<String, String> properties,
                                        @Nullable Map<String, String> postParameters,
                                        @NonNull HttpConnection.RequestMethod method)
            throws AuthorizationException {
        return createAuthorizedRequest(uri, properties, postParameters, method).executeRequest();
    }

    AuthorizedRequest userProfileRequest() {
        if (mOidcConfig.isOAuth2Configuration()) {
            throw new UnsupportedOperationException("Invalid operation. " +
                    "Please check your configuration. OAuth2 authorization servers does not" +
                    "support /userinfo endpoint ");
        }
        return (AuthorizedRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.PROFILE)
                .connectionFactory(mConnectionFactory)
                .tokenResponse(mOktaState.getTokenResponse())
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public UserInfo getUserProfile() throws AuthorizationException {
        JSONObject userInfo = userProfileRequest().executeRequest();
        return new UserInfo(userInfo);
    }

    IntrospectRequest introspectTokenRequest(String token, String tokenType) {
        return (IntrospectRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.INTROSPECT)
                .connectionFactory(mConnectionFactory)
                .introspect(token, tokenType)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public IntrospectInfo introspectToken(String token, String tokenType)
            throws AuthorizationException {
        return introspectTokenRequest(token, tokenType).executeRequest();
    }

    RevokeTokenRequest revokeTokenRequest(String token) {
        return (RevokeTokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REVOKE_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenToRevoke(token)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public Boolean revokeToken(String token) throws AuthorizationException {
        return revokeTokenRequest(token).executeRequest();
    }

    RefreshTokenRequest refreshTokenRequest() {
        return (RefreshTokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REFRESH_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenResponse(mOktaState.getTokenResponse())
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .config(mOidcConfig)
                .createRequest();
    }

    @Override
    public Tokens refreshToken() throws AuthorizationException {
        TokenResponse tokenResponse = refreshTokenRequest().executeRequest();
        mOktaState.save(tokenResponse);
        return new Tokens(tokenResponse);
    }

    @Override
    public Tokens getTokens() {
        TokenResponse response = mOktaState.getTokenResponse();
        if (response == null) {
            return null;
        }
        return new Tokens(response);
    }

    @Override
    public boolean isAuthenticated() {
        TokenResponse tokenResponse = mOktaState.getTokenResponse();
        return tokenResponse != null &&
                (tokenResponse.getAccessToken() != null || tokenResponse.getIdToken() != null);
    }

    @Override
    public void clear() {
        mOktaState.delete(mOktaState.getProviderConfiguration());
        mOktaState.delete(mOktaState.getTokenResponse());
        mOktaState.delete(mOktaState.getAuthorizeRequest());
        mOktaState.setCurrentState(IDLE);
    }

    OktaState getOktaState() {
        return mOktaState;
    }
}
