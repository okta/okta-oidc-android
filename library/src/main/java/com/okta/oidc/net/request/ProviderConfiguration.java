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

@SuppressWarnings("unused")
public class ProviderConfiguration {

    public static final String OPENID_CONFIGURATION_RESOURCE = "/.well-known/openid-configuration";

    static final String OAUTH2_CONFIGURATION_RESOURCE = "/.well-known/oauth-authorization-server";

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

    ProviderConfiguration() {
        //NO-OP
    }

    void validate() throws MissingArgumentException {
        if (authorization_endpoint == null) {
            throw new MissingArgumentException("endpoint");
        }
        //TODO add more checks
    }

    public static class MissingArgumentException extends Exception {
        private String mMissingField;

        public MissingArgumentException(String field) {
            super("Missing mandatory configuration field: " + field);
            mMissingField = field;
        }

        public String getMissingField() {
            return mMissingField;
        }
    }
}
