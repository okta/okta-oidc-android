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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.okta.oidc.storage.Persistable;

import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("unused")
public class ProviderConfiguration implements Persistable {
    public static final String OPENID_CONFIGURATION_RESOURCE = "/.well-known/openid-configuration";

    public static final String OAUTH2_CONFIGURATION_RESOURCE = "/.well-known/oauth-authorization-server";

    public String authorization_endpoint;

    public String[] claims_supported;

    public String[] code_challenge_methods_supported;

    public String end_session_endpoint;

    public String[] grant_types_supported;

    public String introspection_endpoint;

    public String[] introspection_endpoint_auth_methods_supported;

    public String issuer;

    public String jwks_uri;

    public String registration_endpoint;

    public String[] request_object_signing_alg_values_supported;

    public boolean request_parameter_supported;

    public String[] response_modes_supported;

    public String[] response_types_supported;

    public String revocation_endpoint;

    public String[] revocation_endpoint_auth_methods_supported;

    public String[] scopes_supported;

    public String[] subject_types_supported;

    public String token_endpoint;

    public String[] token_endpoint_auth_methods_supported;

    public String userinfo_endpoint;

    public String[] id_token_signing_alg_values_supported;

    @VisibleForTesting
    public ProviderConfiguration() {
        //NO-OP
    }

    void validate() throws IllegalArgumentException {
        if (TextUtils.isEmpty(authorization_endpoint)) {
            throw new IllegalArgumentException("authorization_endpoint is missing");
        }
        if (TextUtils.isEmpty(end_session_endpoint)) {
            throw new IllegalArgumentException("end_session_endpoint is missing");
        }
        if (TextUtils.isEmpty(introspection_endpoint)) {
            throw new IllegalArgumentException("introspection_endpoint is missing");
        }
        if (TextUtils.isEmpty(issuer)) {
            throw new IllegalArgumentException("issuer is missing");
        }
        if (TextUtils.isEmpty(jwks_uri)) {
            throw new IllegalArgumentException("jwks_uri is missing");
        }
        if (TextUtils.isEmpty(registration_endpoint)) {
            throw new IllegalArgumentException("registration_endpoint is missing");
        }
        if (TextUtils.isEmpty(revocation_endpoint)) {
            throw new IllegalArgumentException("revocation_endpoint is missing");
        }
        if (TextUtils.isEmpty(token_endpoint)) {
            throw new IllegalArgumentException("token_endpoint is missing");
        }
        if (TextUtils.isEmpty(userinfo_endpoint)) {
            throw new IllegalArgumentException("userinfo_endpoint is missing");
        }
    }

    public static final Persistable.Restore<ProviderConfiguration> RESTORE =
            new Persistable.Restore<ProviderConfiguration>() {
                private static final String KEY = "ProviderConfiguration";

                @NonNull
                @Override
                public String getKey() {
                    return KEY;
                }

                @Override
                public ProviderConfiguration restore(@Nullable String data) {
                    if (data != null) {
                        return new Gson().fromJson(data, ProviderConfiguration.class);
                    }
                    return null;
                }

                @Override
                public boolean encrypted() {
                    return false;
                }
            };

    @Override
    public boolean encrypt() {
        return RESTORE.encrypted();
    }

    @NonNull
    @Override
    public String getKey() {
        return RESTORE.getKey();
    }

    @Override
    public String persist() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProviderConfiguration that = (ProviderConfiguration) o;

        if (request_parameter_supported != that.request_parameter_supported) return false;
        if (authorization_endpoint != null ? !authorization_endpoint.equals(that.authorization_endpoint) : that.authorization_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(claims_supported, that.claims_supported)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(code_challenge_methods_supported, that.code_challenge_methods_supported))
            return false;
        if (end_session_endpoint != null ? !end_session_endpoint.equals(that.end_session_endpoint) : that.end_session_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(grant_types_supported, that.grant_types_supported)) return false;
        if (introspection_endpoint != null ? !introspection_endpoint.equals(that.introspection_endpoint) : that.introspection_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(introspection_endpoint_auth_methods_supported, that.introspection_endpoint_auth_methods_supported))
            return false;
        if (issuer != null ? !issuer.equals(that.issuer) : that.issuer != null) return false;
        if (jwks_uri != null ? !jwks_uri.equals(that.jwks_uri) : that.jwks_uri != null)
            return false;
        if (registration_endpoint != null ? !registration_endpoint.equals(that.registration_endpoint) : that.registration_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(request_object_signing_alg_values_supported, that.request_object_signing_alg_values_supported))
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(response_modes_supported, that.response_modes_supported))
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(response_types_supported, that.response_types_supported))
            return false;
        if (revocation_endpoint != null ? !revocation_endpoint.equals(that.revocation_endpoint) : that.revocation_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(revocation_endpoint_auth_methods_supported, that.revocation_endpoint_auth_methods_supported))
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(scopes_supported, that.scopes_supported)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(subject_types_supported, that.subject_types_supported))
            return false;
        if (token_endpoint != null ? !token_endpoint.equals(that.token_endpoint) : that.token_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(token_endpoint_auth_methods_supported, that.token_endpoint_auth_methods_supported))
            return false;
        if (userinfo_endpoint != null ? !userinfo_endpoint.equals(that.userinfo_endpoint) : that.userinfo_endpoint != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(id_token_signing_alg_values_supported, that.id_token_signing_alg_values_supported);
    }
}