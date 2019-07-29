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

package com.okta.oidc.net.request;

import android.net.Uri;

import androidx.annotation.RestrictTo;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.params.GrantTypes;
import com.okta.oidc.net.params.RequestType;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.util.AuthorizationException;

import java.util.Map;

import static com.okta.oidc.net.params.RequestType.AUTHORIZED;
import static com.okta.oidc.net.params.RequestType.CONFIGURATION;
import static com.okta.oidc.net.params.RequestType.INTROSPECT;
import static com.okta.oidc.net.params.RequestType.PROFILE;
import static com.okta.oidc.net.params.RequestType.REFRESH_TOKEN;
import static com.okta.oidc.net.params.RequestType.REVOKE_TOKEN;
import static com.okta.oidc.net.params.RequestType.TOKEN_EXCHANGE;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class HttpRequestBuilder {

    public static Authorized newAuthorizedRequest() {
        return new Authorized().requestType(AUTHORIZED);
    }

    public static Configuration newConfigurationRequest() {
        return new Configuration().requestType(CONFIGURATION);
    }

    public static TokenExchange newTokenRequest() {
        return new TokenExchange().requestType(TOKEN_EXCHANGE);
    }

    public static RevokeToken newRevokeTokenRequest() {
        return new RevokeToken().requestType(REVOKE_TOKEN);
    }

    public static Profile newProfileRequest() {
        return new Profile().requestType(PROFILE);
    }

    public static RefreshToken newRefreshTokenRequest() {
        return new RefreshToken().requestType(REFRESH_TOKEN);
    }

    public static Introspect newIntrospectRequest() {
        return new Introspect().requestType(INTROSPECT);
    }

    private abstract static class Builder<T extends Builder<T>> {
        OIDCConfig mConfig;
        ProviderConfiguration mProviderConfiguration;
        RequestType mRequestType;

        /*
         * prevent unchecked cast warning.
         */
        abstract T toThis();

        protected void validate(boolean isConfigurationRequest) throws AuthorizationException {
            if (mConfig == null) {
                throwException("Invalid config");
            }
            if (mProviderConfiguration == null && !isConfigurationRequest) {
                throwException("Missing service configuration");
            }
        }

        public T config(OIDCConfig config) {
            mConfig = config;
            return toThis();
        }

        public T providerConfiguration(ProviderConfiguration providerConfiguration) {
            mProviderConfiguration = providerConfiguration;
            return toThis();
        }

        public T requestType(RequestType requestType) {
            mRequestType = requestType;
            return toThis();
        }

        public abstract HttpRequest createRequest() throws AuthorizationException;
    }

    public static class Configuration extends Builder<Configuration> {
        private Configuration() {
        }

        @Override
        Configuration toThis() {
            return this;
        }

        @Override
        public ConfigurationRequest createRequest() throws AuthorizationException {
            validate(true);
            return new ConfigurationRequest(this);
        }
    }

    public static class Authorized extends Builder<Authorized> {
        Uri mUri;
        TokenResponse mTokenResponse;
        Map<String, String> mPostParameters;
        Map<String, String> mProperties;
        byte[] mRequestBody;
        ConnectionParameters.RequestMethod mRequestMethod;

        private Authorized() {
        }

        @Override
        Authorized toThis() {
            return this;
        }

        protected void validate(boolean isConfigurationRequest) throws AuthorizationException {
            super.validate(isConfigurationRequest);
            if (mTokenResponse == null || mTokenResponse.getIdToken() == null || mUri == null) {
                throwException("Not logged in or invalid uri");
            }
        }

        public Authorized uri(Uri uri) {
            mUri = uri;
            return this;
        }

        public Authorized tokenResponse(TokenResponse tokenResponse) {
            mTokenResponse = tokenResponse;
            return this;
        }

        public Authorized postParameters(Map<String, String> postParameters) {
            mPostParameters = postParameters;
            return this;
        }

        public Authorized properties(Map<String, String> properties) {
            mProperties = properties;
            return this;
        }

        public Authorized httpRequestMethod(ConnectionParameters.RequestMethod requestMethod) {
            mRequestMethod = requestMethod;
            return this;
        }

        public Authorized requestBody(byte[] requestBody) {
            mRequestBody = requestBody;
            return this;
        }

        @Override
        public AuthorizedRequest createRequest() throws AuthorizationException {
            validate(false);
            return new AuthorizedRequest(this);
        }
    }

    public static class TokenExchange extends Builder<TokenExchange> {
        AuthorizeRequest mAuthRequest;
        AuthorizeResponse mAuthResponse;
        String mGrantType;

        private TokenExchange() {
        }

        @Override
        TokenExchange toThis() {
            return this;
        }

        @Override
        protected void validate(boolean isConfigurationRequest) throws AuthorizationException {
            super.validate(isConfigurationRequest);
            if (mAuthRequest == null || mAuthResponse == null) {
                throwException("Missing auth request or response");
            }
        }

        public TokenExchange authRequest(AuthorizeRequest authRequest) {
            mAuthRequest = authRequest;
            return this;
        }

        public TokenExchange authResponse(AuthorizeResponse authResponse) {
            mAuthResponse = authResponse;
            return this;
        }

        @Override
        public TokenRequest createRequest() throws AuthorizationException {
            validate(false);
            mGrantType = GrantTypes.AUTHORIZATION_CODE;
            return new TokenRequest(this);
        }
    }

    public static class RefreshToken extends Builder<RefreshToken> {
        TokenResponse mTokenResponse;
        String mGrantType;

        private RefreshToken() {
        }

        @Override
        RefreshToken toThis() {
            return this;
        }

        public RefreshToken tokenResponse(TokenResponse tokenResponse) {
            mTokenResponse = tokenResponse;
            return this;
        }

        @Override
        protected void validate(boolean isConfigurationRequest) throws AuthorizationException {
            super.validate(isConfigurationRequest);
            if (mTokenResponse == null || mTokenResponse.getRefreshToken() == null) {
                throwException("No refresh token found");
            }
        }

        @Override
        public RefreshTokenRequest createRequest() throws AuthorizationException {
            validate(false);
            mGrantType = GrantTypes.REFRESH_TOKEN;
            return new RefreshTokenRequest(this);
        }
    }

    public static class RevokeToken extends Builder<RevokeToken> {
        String mTokenToRevoke;

        private RevokeToken() {
        }

        @Override
        RevokeToken toThis() {
            return this;
        }

        @Override
        protected void validate(boolean isConfigurationRequest) throws AuthorizationException {
            super.validate(isConfigurationRequest);
            if (mTokenToRevoke == null) {
                throwException("Invalid token");
            }
        }

        public RevokeToken tokenToRevoke(String token) {
            mTokenToRevoke = token;
            return this;
        }

        @Override
        public RevokeTokenRequest createRequest() throws AuthorizationException {
            validate(false);
            return new RevokeTokenRequest(this);
        }
    }

    public static class Profile extends Builder<Profile> {
        TokenResponse mTokenResponse;

        private Profile() {
        }

        @Override
        Profile toThis() {
            return this;
        }

        public Profile tokenResponse(TokenResponse tokenResponse) {
            mTokenResponse = tokenResponse;
            return this;
        }

        @Override
        public AuthorizedRequest createRequest() throws AuthorizationException {
            Authorized authorized = newAuthorizedRequest().requestType(PROFILE);
            authorized.tokenResponse(mTokenResponse);
            authorized.config(mConfig);
            authorized.providerConfiguration(mProviderConfiguration);
            if (mProviderConfiguration != null) {
                authorized.uri(Uri.parse(mProviderConfiguration.userinfo_endpoint));
            }
            authorized.httpRequestMethod(ConnectionParameters.RequestMethod.POST);
            authorized.validate(false);
            return new AuthorizedRequest(authorized);
        }
    }

    public static class Introspect extends Builder<Introspect> {
        String mIntrospectToken;
        String mTokenTypeHint;

        private Introspect() {
        }

        @Override
        Introspect toThis() {
            return this;
        }

        @Override
        protected void validate(boolean isConfigurationRequest) throws AuthorizationException {
            super.validate(isConfigurationRequest);
            if (mIntrospectToken == null || mTokenTypeHint == null) {
                throwException("Invalid token or missing hint");
            }
        }

        public Introspect introspect(String token, String tokenType) {
            mIntrospectToken = token;
            mTokenTypeHint = tokenType;
            return this;
        }

        @Override
        public IntrospectRequest createRequest() throws AuthorizationException {
            validate(false);
            return new IntrospectRequest(this);
        }
    }

    private static void throwException(String message) throws AuthorizationException {
        throw new AuthorizationException(message,
                new RuntimeException());

    }
}
