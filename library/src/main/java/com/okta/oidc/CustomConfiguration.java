/*
 * Copyright (c) 2020, Okta, Inc. and/or its affiliates. All rights reserved.
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

import android.text.TextUtils;

import androidx.annotation.NonNull;

/**
 * Used for authorization servers that does not provide a discovery endpoint.
 * It is recommended to use {@link com.okta.oidc.OIDCConfig.Builder#discoveryUri(String)}
 * to fetch the Open ID provider configuration instead.
 *
 * @see "OpenID provider issuer discovery <https://openid.net/specs/openid-connect-discovery-1_0.html#IssuerDiscovery"
 */
@SuppressWarnings("unused")
public class CustomConfiguration {
    private String mAuthorizationEndpoint;
    private String mTokenEndpoint;
    private String mUserInfoEndpoint;
    private String mJwksUri;
    private String mRegistrationEndpoint;
    private String mIntrospectionEndpoint;
    private String mRevocationEndpoint;
    private String mEndSessionEndpoint;

    /**
     * Gets authorization endpoint.
     *
     * @return the authorization endpoint
     */
    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    /**
     * Gets token endpoint.
     *
     * @return the token endpoint
     */
    public String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    /**
     * Gets user info endpoint.
     *
     * @return the user info endpoint
     */
    public String getUserInfoEndpoint() {
        return mUserInfoEndpoint;
    }

    /**
     * Gets jwks uri.
     *
     * @return the jwks uri
     */
    public String getJwksUri() {
        return mJwksUri;
    }

    /**
     * Gets registration endpoint.
     *
     * @return the registration endpoint
     */
    public String getRegistrationEndpoint() {
        return mRegistrationEndpoint;
    }

    /**
     * Gets introspection endpoint.
     *
     * @return the introspection endpoint
     */
    public String getIntrospectionEndpoint() {
        return mIntrospectionEndpoint;
    }

    /**
     * Gets revocation endpoint.
     *
     * @return the revocation endpoint
     */
    public String getRevocationEndpoint() {
        return mRevocationEndpoint;
    }

    /**
     * Gets end session endpoint.
     *
     * @return the end session endpoint
     */
    public String getEndSessionEndpoint() {
        return mEndSessionEndpoint;
    }

    private CustomConfiguration() {
    }

    /**
     * The CustomConfiguration Builder for setting endpoints for Okta OpenID provider.
     *
     * @see "OpenID Connect & OAuth 2.0 API <https://developer.okta.com/docs/reference/api/oidc/#endpoints"
     */
    public static class Builder {
        private CustomConfiguration mConfiguration;

        /**
         * Instantiates a new Builder for CustomConfiguration.
         */
        public Builder() {
            mConfiguration = new CustomConfiguration();
        }

        /**
         * Required authorization endpoint.
         *
         * @param authorizationEndpoint the authorization endpoint
         * @return the builder
         */
        public Builder authorizationEndpoint(@NonNull String authorizationEndpoint) {
            mConfiguration.mAuthorizationEndpoint = authorizationEndpoint;
            return this;
        }

        /**
         * Required token endpoint.
         *
         * @param tokenEndpoint the token endpoint
         * @return the builder
         */
        public Builder tokenEndpoint(@NonNull String tokenEndpoint) {
            mConfiguration.mTokenEndpoint = tokenEndpoint;
            return this;
        }

        /**
         * Optional user info endpoint.
         *
         * @param userInfoEndpoint the user info endpoint
         * @return the builder
         */
        public Builder userInfoEndpoint(@NonNull String userInfoEndpoint) {
            mConfiguration.mUserInfoEndpoint = userInfoEndpoint;
            return this;
        }

        /**
         * Optional Jwks uri.
         *
         * @param jwksUri the jwks uri
         * @return the builder
         */
        public Builder jwksUri(@NonNull String jwksUri) {
            mConfiguration.mJwksUri = jwksUri;
            return this;
        }

        /**
         * Optional registration endpoint.
         *
         * @param registrationEndpoint the registration endpoint
         * @return the builder
         */
        public Builder registrationEndpoint(@NonNull String registrationEndpoint) {
            mConfiguration.mRegistrationEndpoint = registrationEndpoint;
            return this;
        }

        /**
         * Optional introspection endpoint.
         *
         * @param introspectionEndpoint the introspection endpoint
         * @return the builder
         */
        public Builder introspectionEndpoint(@NonNull String introspectionEndpoint) {
            mConfiguration.mIntrospectionEndpoint = introspectionEndpoint;
            return this;
        }

        /**
         * Optional revocation endpoint.
         *
         * @param revocationEndpoint the revocation endpoint
         * @return the builder
         */
        public Builder revocationEndpoint(@NonNull String revocationEndpoint) {
            mConfiguration.mRevocationEndpoint = revocationEndpoint;
            return this;
        }

        /**
         * Optional end session endpoint.
         *
         * @param endSessionEndpoint the end session endpoint
         * @return the builder
         */
        public Builder endSessionEndpoint(@NonNull String endSessionEndpoint) {
            mConfiguration.mEndSessionEndpoint = endSessionEndpoint;
            return this;
        }

        /**
         * Create the custom configuration.
         *
         * @return Custom configuration
         * @throws IllegalStateException - If missing required endpoints.
         */
        public CustomConfiguration create() throws IllegalStateException {
            validate();
            return mConfiguration;
        }

        private void validate() {
            if (TextUtils.isEmpty(mConfiguration.getAuthorizationEndpoint())) {
                throw new IllegalStateException("No authorization endpoint specified");
            }
            if (TextUtils.isEmpty(mConfiguration.getTokenEndpoint())) {
                throw new IllegalStateException("No token endpoint specified");
            }
        }
    }
}
