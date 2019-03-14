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
package com.okta.oidc.net.request.web;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.params.ResponseType;
import com.okta.oidc.util.AsciiStringListUtil;
import com.okta.oidc.util.CodeVerifierUtil;

import java.util.Arrays;
import java.util.Map;

//https://developer.okta.com/docs/api/resources/oidc#authorize
@SuppressWarnings("unused")
public class AuthorizeRequest implements WebRequest {
    private Parameters mParameters;

    private AuthorizeRequest(Parameters parameters) {
        mParameters = parameters;
    }

    //Convert the parameters as json to save
    @Override
    public String asJson() {
        return new Gson().toJson(mParameters);
    }

    @Override
    public String getState() {
        if (mParameters != null) {
            return mParameters.state;
        }
        return null;
    }

    public static AuthorizeRequest fromJson(String json) {
        Parameters params = new Gson().fromJson(json, Parameters.class);
        return new AuthorizeRequest(params);
    }

    public String getCodeVerifier() {
        return mParameters.code_verifier;
    }

    public String getNonce() {
        return mParameters.nonce;
    }

    @Override
    public Uri toUri() {
        return mParameters.toUri();
    }

    static class Parameters {
        Parameters() {
            //NO-OP
        }

        String authorize_endpoint; //required
        String client_id; //required
        String code_challenge; //required
        String code_challenge_method; //required
        String display;
        String idp_scope;
        String idp;
        String login_hint;
        String max_age;
        String nonce; //required
        String prompt;
        String redirect_uri; //required
        String response_type; //required
        String response_mode;
        String request; //JWT
        String scope; //required
        String sessionToken;
        String state; //required
        Map<String, String> additionalParams;

        String code_verifier; //required.

        Uri toUri() {
            Uri.Builder uriBuilder = Uri.parse(authorize_endpoint).buildUpon()
                    .appendQueryParameter("redirect_uri", redirect_uri)
                    .appendQueryParameter("client_id", client_id)
                    .appendQueryParameter("response_type", response_type)
                    .appendQueryParameter("code_challenge", code_challenge)
                    .appendQueryParameter("code_challenge_method", code_challenge_method)
                    .appendQueryParameter("nonce", nonce)
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("scope", scope);

            appendOptionalParams(uriBuilder, "display", display);
            appendOptionalParams(uriBuilder, "login_hint", login_hint);
            appendOptionalParams(uriBuilder, "prompt", prompt);
            appendOptionalParams(uriBuilder, "idp", idp);
            appendOptionalParams(uriBuilder, "idp_scope", idp_scope);
            appendOptionalParams(uriBuilder, "max_age", max_age);
            appendOptionalParams(uriBuilder, "response_mode", response_mode);
            appendOptionalParams(uriBuilder, "request", request);
            appendOptionalParams(uriBuilder, "sessionToken", sessionToken);

            if (additionalParams != null) {
                for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                    uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
                }
            }

            return uriBuilder.build();
        }

        private void appendOptionalParams(Uri.Builder builder, String name, String value) {
            if (value != null) {
                builder.appendQueryParameter(name, value);
            }
        }
    }

    public static final class Builder {
        private Parameters mParameters;

        private void setCodeVerifier(String verifier) {
            if (verifier == null) {
                verifier = CodeVerifierUtil.generateRandomCodeVerifier();
            }
            CodeVerifierUtil.checkCodeVerifier(verifier);
            mParameters.code_verifier = verifier;
            mParameters.code_challenge = CodeVerifierUtil.deriveCodeVerifierChallenge(mParameters.code_verifier);
            mParameters.code_challenge_method = CodeVerifierUtil.getCodeVerifierChallengeMethod();
        }

        private void validate() {
            //TODO validate all parameters
        }

        public Builder() {
            mParameters = new Parameters();
            //Set default params
            mParameters.response_type = ResponseType.CODE;
            if (mParameters.state == null) {
                mParameters.state = CodeVerifierUtil.generateRandomState();
            }
            mParameters.nonce = CodeVerifierUtil.generateRandomState();
            setCodeVerifier(null);
        }

        public AuthorizeRequest create() {
            validate();
            return new AuthorizeRequest(mParameters);
        }

        public Builder clientId(@NonNull String clientId) {
            mParameters.client_id = clientId;
            return this;
        }

        public Builder setDisplay(@Nullable String display) {
            mParameters.display = display;
            return this;
        }

        public Builder loginHint(@Nullable String loginHint) {
            mParameters.login_hint = loginHint;
            return this;
        }

        public Builder codeVerifier(@Nullable String codeVerifier) {
            setCodeVerifier(codeVerifier);
            return this;
        }

        public Builder idp(@Nullable String idp) {
            mParameters.idp = idp;
            return this;
        }

        /*
        A space delimited list of scopes to be provided to the Social Identity Provider when performing Social Login.
        These scopes are used in addition to the scopes already configured on the Identity Provider.
         */
        public Builder idpScopes(@Nullable String idp_scope) {
            mParameters.idp_scope = idp_scope;
            return this;
        }

        public Builder idpScopes(@Nullable String... idp_scope) {
            if (idp_scope != null) {
                mParameters.idp_scope = AsciiStringListUtil.iterableToString(Arrays.asList(idp_scope));
            }
            return this;
        }

        public Builder maxAge(@Nullable String maxAge) {
            mParameters.max_age = maxAge;
            return this;
        }

        public Builder nonce(@NonNull String nonce) {
            mParameters.nonce = nonce;
            return this;
        }

        //TODO enforce interface for prompt
        public Builder prompt(@Nullable String prompt) {
            mParameters.prompt = prompt;
            return this;
        }

        public Builder authorizeEndpoint(@NonNull String endpoint) {
            mParameters.authorize_endpoint = endpoint;
            return this;
        }

        public Builder redirectUri(@NonNull String redirectUri) {
            mParameters.redirect_uri = redirectUri;
            return this;
        }

        public Builder responseType(@NonNull String responseType) {
            mParameters.response_type = responseType;
            return this;
        }

        //TODO
        public Builder responseMode(@Nullable String responseMode) {
            mParameters.response_mode = responseMode;
            return this;
        }

        /*
        A JWT created by the client that enables requests to be passed as a single,
        self-contained parameter. See Parameter Details for more.
         */
        public Builder requestJWT(@Nullable String request) {
            mParameters.request = request;
            return this;
        }

        public Builder scope(@NonNull String scope) {
            mParameters.scope = scope;
            return this;
        }

        public Builder scope(@NonNull String... scopes) {
            mParameters.idp_scope = AsciiStringListUtil.iterableToString(Arrays.asList(scopes));
            return this;
        }

        public Builder sessionToken(@Nullable String sessionToken) {
            mParameters.sessionToken = sessionToken;
            return this;
        }

        public Builder state(@NonNull String state) {
            mParameters.state = state;
            return this;
        }

        public Builder additionalParams(@Nullable Map<String, String> params) {
            mParameters.additionalParams = params;
            return this;
        }

        public Builder account(OIDCAccount account) {
            mParameters.authorize_endpoint = account.getProviderConfig().authorization_endpoint;
            mParameters.client_id = account.getClientId();
            mParameters.scope = AsciiStringListUtil
                    .iterableToString(Arrays.asList(account.getScopes()));
            mParameters.redirect_uri = account.getRedirectUri().toString();
            return this;
        }
    }
}
