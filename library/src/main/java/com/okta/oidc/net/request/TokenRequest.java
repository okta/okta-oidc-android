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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaIdToken;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.UriUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TokenRequest extends BaseRequest<TokenResponse, AuthorizationException> {
    private static final String TAG = TokenRequest.class.getSimpleName();

    private String code;
    private String client_assertion;
    private String client_assertion_type;
    protected String client_id;
    private String client_secret;
    private String code_verifier;
    protected String grant_type;
    private String password;
    private String redirect_uri;
    protected String refresh_token;
    protected String scope;
    private String username;
    private String nonce;
    protected OIDCConfig mConfig;
    protected ProviderConfiguration mProviderConfiguration;

    //if set, used to verify idtoken auth_Time
    private String mMaxAge;

    TokenRequest() {
    }

    TokenRequest(HttpRequestBuilder.TokenExchange b) {
        super();
        mRequestType = b.mRequestType;
        mConfig = b.mConfig;
        mProviderConfiguration = b.mProviderConfiguration;
        mUri = Uri.parse(mProviderConfiguration.token_endpoint);
        client_id = b.mConfig.getClientId();
        redirect_uri = b.mConfig.getRedirectUri().toString();
        grant_type = b.mGrantType;
        mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.POST)
                .setRequestProperty("Accept", ConnectionParameters.JSON_CONTENT_TYPE)
                .setPostParameters(buildParameters(b))
                .setRequestType(mRequestType)
                .create();
    }

    public String getGrantType() {
        return grant_type;
    }

    public OIDCConfig getConfig() {
        return mConfig;
    }

    public ProviderConfiguration getProviderConfiguration() {
        return mProviderConfiguration;
    }

    public String getNonce() {
        return nonce;
    }

    @Nullable
    public String getMaxAge() {
        return mMaxAge;
    }

    protected Map<String, String> buildParameters(HttpRequestBuilder.TokenExchange b) {
        code_verifier = b.mAuthRequest.getCodeVerifier();
        nonce = b.mAuthRequest.getNonce();
        code = b.mAuthResponse.getCode();
        mMaxAge = b.mAuthRequest.getMaxAge();
        Map<String, String> params = new HashMap<>();
        params.put("client_id", client_id);
        params.put("grant_type", grant_type);
        params.put("redirect_uri", redirect_uri);
        params.put("code_verifier", code_verifier);
        params.put("code", code);
        params.put("nonce", nonce);
        return params;
    }

    @Override
    public TokenResponse executeRequest(OktaHttpClient client) throws AuthorizationException {
        HttpResponse response = null;
        TokenResponse tokenResponse;
        try {
            response = openConnection(client);
            JSONObject json = response.asJson();
            if (json.has(AuthorizationException.PARAM_ERROR)) {
                try {
                    final String error = json.getString(AuthorizationException.PARAM_ERROR);
                    throw AuthorizationException.fromOAuthTemplate(
                            AuthorizationException.TokenRequestErrors.byString(error),
                            error,
                            json.optString(AuthorizationException.PARAM_ERROR_DESCRIPTION,
                                    null),
                            UriUtil.parseUriIfAvailable(
                                    json.optString(AuthorizationException.PARAM_ERROR_URI)));
                } catch (JSONException jsonEx) {
                    throw AuthorizationException.fromTemplate(
                            AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                            jsonEx);
                }
            }
            tokenResponse = new Gson().fromJson(json.toString(), TokenResponse.class);
            tokenResponse.setCreationTime(System.currentTimeMillis());
            if (tokenResponse.getIdToken() != null) {
                OktaIdToken idToken;
                try {
                    idToken = OktaIdToken.parseIdToken(tokenResponse.getIdToken());
                } catch (IllegalArgumentException | JsonIOException ex) {
                    Log.e(TAG, "", ex);
                    throw AuthorizationException.fromTemplate(
                            AuthorizationException.GeneralErrors.ID_TOKEN_PARSING_ERROR,
                            ex);
                }
                idToken.validate(this, mConfig.getIdTokenValidator());
            }
            return tokenResponse;
        } catch (IOException ex) {
            throw new AuthorizationException(ex.getMessage(), ex);
        } catch (JSONException ex) {
            throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR, ex);
        } catch (AuthorizationException ae) {
            throw ae;
        } catch (Exception e) {
            throw AuthorizationException.fromTemplate(AuthorizationException
                    .GeneralErrors.NETWORK_ERROR, e);
        } finally {
            if (response != null) {
                response.disconnect();
            }
        }
    }
}
