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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.okta.oidc.storage.Persistable;

@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ProviderConfiguration implements Persistable {
    public static final String OPENID_CONFIGURATION_RESOURCE = "/.well-known/openid-configuration";

    public static final String OAUTH2_CONFIGURATION_RESOURCE =
            "/.well-known/oauth-authorization-server";

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

    void validate(boolean isOAuth2) throws IllegalArgumentException {
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
        //Oauth2 server configuration doesn't contain userinfo_endpoint.
        if (!isOAuth2 && TextUtils.isEmpty(userinfo_endpoint)) {
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

            };

    @NonNull
    @Override
    public String getKey() {
        return RESTORE.getKey();
    }

    @Override
    public String persist() {
        return new Gson().toJson(this);
    }
}
