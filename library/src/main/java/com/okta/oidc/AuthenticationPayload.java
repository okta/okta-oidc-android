/*
 * Copyright (c) 2018, Okta, Inc. and/or its affiliates. All rights reserved.
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

package com.okta.oidc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * That is payload that is used in order to provide additional parameters
 * for Authorization request or alter default parameters values (like mState).
 *
 */
public class AuthenticationPayload {

    private String mState;
    private String mLoginHint;
    private Map<String, String> mAdditionalParameters;

    private AuthenticationPayload(String state,
                                  String loginHint,
                                  Map<String, String> additionalParameters) {
        this.mState = state;
        this.mLoginHint = loginHint;
        this.mAdditionalParameters = additionalParameters;
    }

    /**
     * Creates instances of {@link AuthenticationPayload}.
     */
    public static class Builder {

        @Nullable
        private String mState;

        @Nullable
        private String mLoginHint;

        private Map<String, String> mAdditionalParameters = new HashMap<>();

        /**
         * Specifies the opaque value used by the client to maintain mState between the request and
         * callback. If this value is not explicitly set, this library will automatically add mState
         * and perform appropriate validation of the mState in the authorization response. It is
         * recommended that the default implementation of this parameter be used wherever possible.
         * Typically used to prevent CSRF attacks, as recommended in
         * [RFC6819 Section 5.3.5](https://tools.ietf.org/html/rfc6819#section-5.3.5).
         *
         * @param state state value
         * @return current Builder
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 4.1.1
         * <https://tools.ietf.org/html/rfc6749#section-4.1.1>"
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 5.3.5
         * <https://tools.ietf.org/html/rfc6749#section-5.3.5>"
         */
        public Builder setState(String state) {
            this.mState = state;
            return this;
        }

        /**
         * Specifies the OpenID Connect 1.0 `login_hint` parameter.
         *
         * @param loginHint login hint value
         * @return current Builder
         *
         * @see "OpenID Connect Core 1.0, Section 3.1.2.1
         * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.3.1.2.1>"
         */
        public Builder setLoginHint(String loginHint) {
            this.mLoginHint = loginHint;
            return this;
        }

        /**
         * Specifies additional parameter. Replaces any previously provided set of parameters.
         * Parameter keys and values cannot be null or empty.
         *
         * @param name  parameter name.
         * @param value parameter value
         * @return current Builder
         *
         * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1
         * <https://tools.ietf.org/html/rfc6749#section-3.1>"
         */
        public Builder addParameter(@NonNull String name, @NonNull String value) {
            mAdditionalParameters.put(name, value);
            return this;
        }

        /**
         * Constructs a new instance of {@link AuthenticationPayload}.
         *
         * @return constructed authentication payload
         */
        public AuthenticationPayload build() {
            return new AuthenticationPayload(mState, mLoginHint, mAdditionalParameters);
        }

    }

    /**
     * State getter.
     *
     * @return current state
     */
    public String getState() {
        return mState;
    }

    /**
     * Login hint getter.
     *
     * @return current login hint
     */
    public String getLoginHint() {
        return mLoginHint;
    }

    /**
     * Additional Parameters getter.
     *
     * @return additional parameter
     */
    public Map<String, String> getAdditionalParameters() {
        return mAdditionalParameters;
    }
}
