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

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.GrantTypes;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;

import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class HttpRequestBuilder {
    HttpRequest.Type mRequestType;
    @Nullable
    HttpConnectionFactory mConn;
    OIDCConfig mAccount;
    ProviderConfiguration mProviderConfiguration;
    AuthorizeRequest mAuthRequest;
    AuthorizeResponse mAuthResponse;
    Map<String, String> mPostParameters;
    Map<String, String> mProperties;
    Uri mUri;
    HttpConnection.RequestMethod mRequestMethod;
    String mTokenToRevoke;
    TokenResponse mTokenResponse;
    String mGrantType;

    String mIntrospectToken;
    String mTokenTypeHint;

    private HttpRequestBuilder() {
    }

    private void validate(HttpRequest.Type type) {
        if (mAccount == null) {
            throw new IllegalStateException("Invalid account");
        }
        if (mProviderConfiguration == null && type != HttpRequest.Type.CONFIGURATION) {
            throw new IllegalStateException("Missing service configuration");
        }
        switch (type) {
            case CONFIGURATION:
                break; //NO-OP
            case AUTHORIZED:
                if (mTokenResponse == null || mTokenResponse.getAccessToken() == null
                        || mTokenResponse.getIdToken() == null || mUri == null) {
                    throw new IllegalStateException("Not logged in or invalid uri");
                }
                break;
            case TOKEN_EXCHANGE:
                if (mAuthRequest == null || mAuthResponse == null) {
                    throw new IllegalStateException("Missing auth request or response");
                }
                break;
            case PROFILE:
                if (mTokenResponse == null || mTokenResponse.getAccessToken() == null
                        || mTokenResponse.getIdToken() == null) {
                    throw new IllegalStateException("Not logged in");
                }
                break;
            case REVOKE_TOKEN:
                if (mTokenToRevoke == null) {
                    throw new IllegalStateException("Invalid token");
                }
                break;
            case REFRESH_TOKEN:
                if (mTokenResponse == null || mTokenResponse.getRefreshToken() == null
                        || mTokenResponse.getScope() == null) {
                    throw new IllegalStateException("No refresh token found");
                }
                break;
            case INTROSPECT:
                if (mIntrospectToken == null || mTokenTypeHint == null) {
                    throw new IllegalStateException("Invalid token or missing hint");
                }
                break;
            default:
        }
    }

    public static HttpRequestBuilder newRequest() {
        return new HttpRequestBuilder();
    }

    public HttpRequest createRequest() {
        validate(mRequestType);
        switch (mRequestType) {
            case CONFIGURATION:
                return new ConfigurationRequest(this);
            case TOKEN_EXCHANGE:
                mGrantType = GrantTypes.AUTHORIZATION_CODE;
                return new TokenRequest(this);
            case AUTHORIZED:
                return new AuthorizedRequest(this);
            case PROFILE:
                mUri = Uri.parse(mProviderConfiguration.userinfo_endpoint);
                mRequestMethod = HttpConnection.RequestMethod.POST;
                return new AuthorizedRequest(this);
            case REVOKE_TOKEN:
                return new RevokeTokenRequest(this);
            case REFRESH_TOKEN:
                mGrantType = GrantTypes.REFRESH_TOKEN;
                return new RefreshTokenRequest(this);
            case INTROSPECT:
                return new IntrospectRequest(this);
            default:
                throw new IllegalArgumentException("Invalid request of type: " + mRequestType);
        }
    }

    public HttpRequestBuilder request(HttpRequest.Type type) {
        mRequestType = type;
        return this;
    }

    public HttpRequestBuilder connectionFactory(HttpConnectionFactory conn) {
        mConn = conn;
        return this;
    }

    public HttpRequestBuilder account(OIDCConfig account) {
        mAccount = account;
        return this;
    }

    public HttpRequestBuilder providerConfiguration(ProviderConfiguration providerConfiguration) {
        mProviderConfiguration = providerConfiguration;
        return this;
    }

    public HttpRequestBuilder tokenResponse(TokenResponse tokenResponse) {
        mTokenResponse = tokenResponse;
        return this;
    }

    public HttpRequestBuilder authRequest(AuthorizeRequest authRequest) {
        mAuthRequest = authRequest;
        return this;
    }

    public HttpRequestBuilder authResponse(AuthorizeResponse authResponse) {
        mAuthResponse = authResponse;
        return this;
    }

    public HttpRequestBuilder postParameters(Map<String, String> postParameters) {
        mPostParameters = postParameters;
        return this;
    }

    public HttpRequestBuilder properties(Map<String, String> properties) {
        mProperties = properties;
        return this;
    }

    public HttpRequestBuilder uri(Uri uri) {
        mUri = uri;
        return this;
    }

    public HttpRequestBuilder httpRequestMethod(HttpConnection.RequestMethod requestMethod) {
        mRequestMethod = requestMethod;
        return this;
    }

    public HttpRequestBuilder tokenToRevoke(String token) {
        mTokenToRevoke = token;
        return this;
    }

    public HttpRequestBuilder introspect(String token, String tokenType) {
        mIntrospectToken = token;
        mTokenTypeHint = tokenType;
        return this;
    }
}
