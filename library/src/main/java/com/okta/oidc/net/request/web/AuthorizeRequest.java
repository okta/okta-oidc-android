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
import android.text.TextUtils;

import com.google.gson.Gson;
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.ResponseType;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.util.AsciiStringListUtil;
import com.okta.oidc.util.CodeVerifierUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

//https://developer.okta.com/docs/api/resources/oidc#authorize
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("unused")
public class AuthorizeRequest extends WebRequest {
    private Parameters mParameters;

    public AuthorizeRequest(Parameters parameters) {
        mParameters = parameters;
    }

    @Override
    public String getState() {
        return checkIfReplaced("state", mParameters.state);
    }

    public String getCodeVerifier() {
        return checkIfReplaced("code_verifier", mParameters.code_verifier);
    }

    public String getNonce() {
        return checkIfReplaced("nonce", mParameters.nonce);
    }

    public String getMaxAge() {
        return checkIfReplaced("max_age", mParameters.max_age);
    }

    @Nullable
    private String checkIfReplaced(@NonNull String key, @Nullable String value) {
        String retVal = value;
        if (mParameters.additionalParams != null) {
            String replace = mParameters.additionalParams.get(key);
            retVal = replace == null ? value : replace;
        }
        return retVal;
    }

    @Override
    @NonNull
    public Uri toUri() {
        return mParameters.toUri();
    }

    @NonNull
    public String getKey() {
        return RESTORE.getKey();
    }

    @Override
    public String persist() {
        mParameters.request_type = "authorize";
        return new Gson().toJson(mParameters);
    }

    @Override
    public boolean encrypt() {
        return RESTORE.encrypted();
    }

    public static class Parameters {
        Parameters() {
            //NO-OP
        }

        String request_type; //for serializing
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

        public Uri toUri() {
            Map<String, String> requiredParams = new HashMap<>();
            requiredParams.put("redirect_uri", redirect_uri);
            requiredParams.put("client_id", client_id);
            requiredParams.put("response_type", response_type);
            requiredParams.put("code_challenge", code_challenge);
            requiredParams.put("code_challenge_method", code_challenge_method);
            requiredParams.put("nonce", nonce);
            requiredParams.put("state", state);
            requiredParams.put("scope", scope);
            if (additionalParams != null) {
                //replace default params if provided.
                requiredParams.putAll(additionalParams);
            }

            appendOptionalParams(requiredParams, "display", display);
            appendOptionalParams(requiredParams, "login_hint", login_hint);
            appendOptionalParams(requiredParams, "prompt", prompt);
            appendOptionalParams(requiredParams, "idp", idp);
            appendOptionalParams(requiredParams, "idp_scope", idp_scope);
            appendOptionalParams(requiredParams, "max_age", max_age);
            appendOptionalParams(requiredParams, "response_mode", response_mode);
            appendOptionalParams(requiredParams, "request", request);
            appendOptionalParams(requiredParams, "sessionToken", sessionToken);

            Uri.Builder uriBuilder = Uri.parse(authorize_endpoint).buildUpon();
            for (Map.Entry<String, String> entry : requiredParams.entrySet()) {
                uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            return uriBuilder.build();
        }

        private void appendOptionalParams(Map<String, String> map, String name, String value) {
            if (value != null) {
                map.put(name, value);
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
            mParameters.code_challenge = CodeVerifierUtil.
                    deriveCodeVerifierChallenge(mParameters.code_verifier);
            mParameters.code_challenge_method = CodeVerifierUtil.getCodeVerifierChallengeMethod();
        }

        private void validate(boolean isNative) {
            if (TextUtils.isEmpty(mParameters.authorize_endpoint)) {
                throw new IllegalArgumentException("authorize_endpoint missing");
            }
            if (TextUtils.isEmpty(mParameters.code_challenge)) {
                throw new IllegalArgumentException("code_challenge missing");
            }
            if (TextUtils.isEmpty(mParameters.code_challenge_method)) {
                throw new IllegalArgumentException("code_challenge_method missing");
            }
            if (TextUtils.isEmpty(mParameters.nonce)) {
                throw new IllegalArgumentException("nonce missing");
            }
            if (TextUtils.isEmpty(mParameters.redirect_uri)) {
                throw new IllegalArgumentException("redirect_uri missing");
            }
            if (TextUtils.isEmpty(mParameters.response_type)) {
                throw new IllegalArgumentException("response_type missing");
            }
            if (TextUtils.isEmpty(mParameters.scope)) {
                throw new IllegalArgumentException("scope missing");
            }
            if (TextUtils.isEmpty(mParameters.state)) {
                throw new IllegalArgumentException("state missing");
            }
            if (isNative && TextUtils.isEmpty(mParameters.sessionToken)) {
                throw new IllegalArgumentException("sessionToken is missing");
            }
        }

        public Builder() {
            mParameters = new Parameters();
            //Set default params
            mParameters.response_type = ResponseType.CODE;
            mParameters.nonce = CodeVerifierUtil.generateRandomState();
            setCodeVerifier(null);
        }

        public AuthorizeRequest create() {
            if (mParameters.state == null) {
                mParameters.state = CodeVerifierUtil.generateRandomState();
            }
            validate(false);
            return new AuthorizeRequest(mParameters);
        }

        public NativeAuthorizeRequest createNativeRequest(HttpConnectionFactory factory) {
            if (mParameters.state == null) {
                mParameters.state = CodeVerifierUtil.generateRandomState();
            }
            validate(true);
            return new NativeAuthorizeRequest(mParameters, factory);
        }

        public Builder clientId(@NonNull String clientId) {
            mParameters.client_id = clientId;
            return this;
        }

        public Builder setDisplay(@Nullable String display) {
            mParameters.display = display;
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
        A space delimited list of scopes to be provided to the
        Social Identity Provider when performing Social Login.
        These scopes are used in addition to the scopes
        already configured on the Identity Provider.
         */
        public Builder idpScopes(@Nullable String... idp_scope) {
            if (idp_scope != null) {
                mParameters.idp_scope = AsciiStringListUtil.
                        iterableToString(Arrays.asList(idp_scope));
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

        public Builder scope(@NonNull String... scopes) {
            mParameters.scope = AsciiStringListUtil.iterableToString(Arrays.asList(scopes));
            return this;
        }

        public Builder sessionToken(@Nullable String sessionToken) {
            mParameters.sessionToken = sessionToken;
            return this;
        }

        public Builder authenticationPayload(@Nullable AuthenticationPayload payload) {
            if (payload != null) {
                mParameters.state = payload.getState();
                mParameters.login_hint = payload.getLoginHint();
                mParameters.additionalParams = payload.getAdditionalParameters();
            }
            return this;
        }

        public Builder providerConfiguration(@NonNull ProviderConfiguration providerConfiguration) {
            mParameters.authorize_endpoint = providerConfiguration.authorization_endpoint;
            return this;
        }

        public Builder account(OIDCAccount account) {
            mParameters.client_id = account.getClientId();
            mParameters.scope = AsciiStringListUtil
                    .iterableToString(Arrays.asList(account.getScopes()));
            mParameters.redirect_uri = account.getRedirectUri().toString();
            return this;
        }
    }
}
