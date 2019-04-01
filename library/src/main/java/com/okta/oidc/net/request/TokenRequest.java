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
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.params.GrantTypes;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.OktaIdToken;
import com.okta.oidc.util.UriUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class TokenRequest extends BaseRequest<TokenResponse, AuthorizationException> {
    private static final String TAG = TokenRequest.class.getSimpleName();

    private String code;
    private String client_assertion;
    private String client_assertion_type;
    private String client_id;
    private String client_secret;
    private String code_verifier;
    private String grant_type;
    private String password;
    private String redirect_uri;
    private String refresh_token;
    private String scope;
    private String username;
    private String nonce;
    private OIDCAccount mAccount;
    private ProviderConfiguration mProviderConfiguration;

    TokenRequest(HttpRequestBuilder b) {
        super();
        mRequestType = b.mRequestType;
        mAccount = b.mAccount;
        mProviderConfiguration = b.mProviderConfiguration;
        mUri = Uri.parse(mProviderConfiguration.token_endpoint);
        client_id = b.mAccount.getClientId();
        redirect_uri = b.mAccount.getRedirectUri().toString();
        code_verifier = b.mAuthRequest.getCodeVerifier();
        nonce = b.mAuthRequest.getNonce();
        code = b.mAuthResponse.getCode();

        mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.POST)
                .setRequestProperty("Accept", HttpConnection.JSON_CONTENT_TYPE)
                .setPostParameters(buildParameters())
                .create(b.mConn);
    }

    @Override
    public void dispatchRequest(final RequestDispatcher dispatcher,
                                final RequestCallback<TokenResponse,
                                        AuthorizationException> callback) {
        dispatcher.submit(() -> {
            try {
                final TokenResponse response = executeRequest();
                dispatcher.submitResults(() -> callback.onSuccess(response));
            } catch (AuthorizationException ae) {
                dispatcher.submitResults(() -> callback.onError(ae.error, ae));
            }
        });
    }

    public String getGrantType() {
        return grant_type;
    }

    public OIDCAccount getAccount() {
        return mAccount;
    }

    public ProviderConfiguration getProviderConfiguration() {
        return mProviderConfiguration;
    }

    public String getNonce() {
        return nonce;
    }

    private Map<String, String> buildParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", client_id);
        params.put("grant_type", GrantTypes.AUTHORIZATION_CODE);
        params.put("redirect_uri", redirect_uri);
        params.put("code_verifier", code_verifier);
        params.put("code", code);
        params.put("nonce", nonce);
        return params;
    }

    @Override
    public TokenResponse executeRequest() throws AuthorizationException {
        HttpResponse response = null;
        TokenResponse tokenResponse;
        try {
            response = openConnection();
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
                idToken.validate(this, System::currentTimeMillis);
            }
            return tokenResponse;
        } catch (IOException ex) {
            throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.NETWORK_ERROR, ex);
        } catch (JSONException ex) {
            throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR, ex);
        } finally {
            if (response != null) {
                response.disconnect();
            }
        }
    }
}
