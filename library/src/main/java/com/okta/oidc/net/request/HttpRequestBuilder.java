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
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;

import java.util.Map;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class HttpRequestBuilder {
    HttpRequest.Type mRequestType;
    @Nullable
    HttpConnectionFactory mConn;
    OIDCAccount mAccount;
    AuthorizeRequest mAuthRequest;
    AuthorizeResponse mAuthResponse;
    Map<String, String> mPostParameters;
    Map<String, String> mProperties;
    Uri mUri;
    HttpConnection.RequestMethod mRequestMethod;
    String mTokenToRevoke;

    private HttpRequestBuilder() {
    }

    private void validate(HttpRequest.Type type) {
        if (mAccount == null) {
            throw new IllegalStateException("Invalid account");
        }
        switch (type) {
            case CONFIGURATION:
                break; //NO-OP
            case TOKEN_EXCHANGE:
                if (mAccount.getProviderConfig() == null) {
                    throw new IllegalStateException("Account is missing or invalid service config");
                }
                break;
            case AUTHORIZED:
                if (mUri == null || mRequestMethod == null || !mAccount.isLoggedIn()) {
                    throw new IllegalStateException("Invalid uri or http method or not logged in");
                }
                break;
            case PROFILE:
                if (!mAccount.isLoggedIn() || mAccount.getProviderConfig() == null) {
                    throw new IllegalArgumentException("Not authorized or invalid service");
                }
                break;
            case REVOKE_TOKEN:
                if (mAccount.getProviderConfig() == null || mTokenToRevoke == null) {
                    throw new IllegalArgumentException("Invalid config or token");
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
                return new TokenRequest(this);
            case AUTHORIZED:
                return new AuthorizedRequest(this);
            case PROFILE:
                mUri = Uri.parse(mAccount.getProviderConfig().userinfo_endpoint);
                mRequestMethod = HttpConnection.RequestMethod.POST;
                return new AuthorizedRequest(this);
            case REVOKE_TOKEN:
                return new RevokeTokenRequest(this);
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

    public HttpRequestBuilder account(OIDCAccount account) {
        mAccount = account;
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
}
