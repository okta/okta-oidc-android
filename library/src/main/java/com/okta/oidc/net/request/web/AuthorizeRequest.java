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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

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

//https://developer.okta.com/docs/api/resources/oidc#authorize
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("unused")
public class AuthorizeRequest extends WebRequest {
    private static final String TAG = AuthorizeRequest.class.getSimpleName();
    private Parameters mParameters;

    //keys
    private static final String AUTHORIZE_ENDPOINT = "authorize_endpoint";
    private static final String CLIENT_ID = "client_id";
    private static final String CODE_CHALLENGE = "code_challenge";
    private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    private static final String DISPLAY = "display";
    private static final String IDP_SCOPE = "idp_scope";
    private static final String IDP = "idp";
    public static final String LOGIN_HINT = "login_hint";
    private static final String MAX_AGE = "max_age";
    private static final String NONCE = "nonce";
    private static final String PROMPT = "prompt";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String RESPONSE_TYPE = "response_type";
    private static final String RESPONSE_MODE = "response_mode";
    private static final String REQUEST = "request";
    private static final String SCOPE = "scope";
    private static final String SESSION_TOKEN = "sessionToken";
    public static final String STATE = "state";
    private static final String CODE_VERIFIER = "code_verifier";

    public AuthorizeRequest(Parameters parameters) {
        mParameters = parameters;
    }

    @Override
    public String getState() {
        return mParameters.queryParams.get(STATE);
    }

    public String getCodeVerifier() {
        return mParameters.queryParams.get(CODE_VERIFIER);
    }

    public String getNonce() {
        return mParameters.queryParams.get(NONCE);
    }

    public String getMaxAge() {
        return mParameters.queryParams.get(MAX_AGE);
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

    public static class Parameters {
        Parameters() {
            //NO-OP
        }

        String request_type; //for serializing
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> mPayloadParams;

        public Uri toUri() {
            Uri.Builder uriBuilder = Uri.parse(queryParams.get(AUTHORIZE_ENDPOINT))
                    .buildUpon();
            queryParams.remove(AUTHORIZE_ENDPOINT);
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            return uriBuilder.build();
        }
    }

    public static final class Builder {
        private Parameters mParameters;
        private Map<String, String> mMap;

        private void setCodeVerifier(@Nullable String verifier) {
            if (verifier == null) {
                verifier = CodeVerifierUtil.generateRandomCodeVerifier();
            }
            CodeVerifierUtil.checkCodeVerifier(verifier);
            mMap.put(CODE_VERIFIER, verifier);
            String challenge = CodeVerifierUtil.deriveCodeVerifierChallenge(verifier);
            if (challenge != null) {
                mMap.put(CODE_CHALLENGE, challenge);
            }
            String method = CodeVerifierUtil.getCodeVerifierChallengeMethod();
            if (method != null) {
                mMap.put(CODE_CHALLENGE_METHOD, method);
            }
        }

        private void validate(boolean isNative) {
            if (TextUtils.isEmpty(mMap.get(AUTHORIZE_ENDPOINT))) {
                throw new IllegalArgumentException("authorize_endpoint missing");
            }
            if (TextUtils.isEmpty(mMap.get(CODE_CHALLENGE))) {
                throw new IllegalArgumentException("code_challenge missing");
            }
            if (TextUtils.isEmpty(mMap.get(CODE_CHALLENGE_METHOD))) {
                throw new IllegalArgumentException("code_challenge_method missing");
            }
            if (TextUtils.isEmpty(mMap.get(NONCE))) {
                throw new IllegalArgumentException("nonce missing");
            }
            if (TextUtils.isEmpty(mMap.get(REDIRECT_URI))) {
                throw new IllegalArgumentException("redirect_uri missing");
            }
            if (TextUtils.isEmpty(mMap.get(RESPONSE_TYPE))) {
                throw new IllegalArgumentException("response_type missing");
            }
            if (TextUtils.isEmpty(mMap.get(SCOPE))) {
                throw new IllegalArgumentException("scope missing");
            }
            if (TextUtils.isEmpty(mMap.get(STATE))) {
                throw new IllegalArgumentException("state missing");
            }
            if (isNative && TextUtils.isEmpty(mMap.get(SESSION_TOKEN))) {
                throw new IllegalArgumentException("sessionToken is missing");
            }
        }

        public Builder() {
            mParameters = new Parameters();
            //Set default params
            mMap = mParameters.queryParams;
            mMap.put(RESPONSE_TYPE, ResponseType.CODE);
            mMap.put(NONCE, CodeVerifierUtil.generateRandomState());
            mMap.put(STATE, CodeVerifierUtil.generateRandomState());
            setCodeVerifier(null);
        }

        public AuthorizeRequest create() {
            if (mParameters.mPayloadParams != null) {
                mMap.putAll(mParameters.mPayloadParams);
            }
            validate(false);
            return new AuthorizeRequest(mParameters);
        }

        public NativeAuthorizeRequest createNativeRequest(HttpConnectionFactory factory) {
            if (mParameters.mPayloadParams != null) {
                mMap.putAll(mParameters.mPayloadParams);
            }
            validate(true);
            return new NativeAuthorizeRequest(mParameters, factory);
        }

        public Builder clientId(@NonNull String clientId) {
            mMap.put(CLIENT_ID, clientId);
            return this;
        }

        public Builder setDisplay(@NonNull String display) {
            mMap.put(DISPLAY, display);
            return this;
        }

        public Builder codeVerifier(@NonNull String codeVerifier) {
            setCodeVerifier(codeVerifier);
            return this;
        }

        public Builder idp(@NonNull String idp) {
            mMap.put(IDP, idp);
            return this;
        }

        /*
        A space delimited list of scopes to be provided to the
        Social Identity Provider when performing Social Login.
        These scopes are used in addition to the scopes
        already configured on the Identity Provider.
         */
        public Builder idpScopes(@Nullable String... idpScopes) {
            String delimited = AsciiStringListUtil.iterableToString(Arrays.asList(idpScopes));
            if (delimited != null) {
                mMap.put(IDP_SCOPE, delimited);
            }
            return this;
        }

        public Builder maxAge(@NonNull String maxAge) {
            mMap.put(MAX_AGE, maxAge);
            return this;
        }

        public Builder nonce(@NonNull String nonce) {
            mMap.put(NONCE, nonce);
            return this;
        }

        public Builder prompt(@NonNull String prompt) {
            mMap.put(PROMPT, prompt);
            return this;
        }

        public Builder authorizeEndpoint(@NonNull String endpoint) {
            mMap.put(AUTHORIZE_ENDPOINT, endpoint);
            return this;
        }

        public Builder redirectUri(@NonNull String redirectUri) {
            mMap.put(REDIRECT_URI, redirectUri);
            return this;
        }

        public Builder responseType(@NonNull String responseType) {
            mMap.put(RESPONSE_TYPE, responseType);
            return this;
        }

        public Builder responseMode(@NonNull String responseMode) {
            mMap.put(RESPONSE_MODE, responseMode);
            return this;
        }

        /*
        A JWT created by the client that enables requests to be passed as a single,
        self-contained parameter. See Parameter Details for more.
         */
        public Builder requestJWT(@NonNull String request) {
            mMap.put(REQUEST, request);
            return this;
        }

        public Builder scope(@NonNull String... scopes) {
            String delimited = AsciiStringListUtil.iterableToString(Arrays.asList(scopes));
            if (delimited != null) {
                mMap.put(SCOPE, delimited);
            }
            return this;
        }

        public Builder sessionToken(@NonNull String token) {
            mMap.put(SESSION_TOKEN, token);
            return this;
        }

        public Builder authenticationPayload(@Nullable AuthenticationPayload payload) {
            if (payload != null) {
                mParameters.mPayloadParams = payload.getAdditionalParameters();
            }
            return this;
        }

        public Builder providerConfiguration(@NonNull ProviderConfiguration providerConfiguration) {
            mMap.put(AUTHORIZE_ENDPOINT, providerConfiguration.authorization_endpoint);
            return this;
        }

        public Builder account(OIDCAccount account) {
            mMap.put(CLIENT_ID, account.getClientId());
            String delimited = AsciiStringListUtil.iterableToString(Arrays.asList(account.getScopes()));
            if (delimited != null) {
                mMap.put(SCOPE, delimited);
            }
            mMap.put(REDIRECT_URI, account.getRedirectUri().toString());
            return this;
        }
    }
}
