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
package com.okta.oidc.util;

public interface JsonStrings {
    String FIRE_FOX = "org.mozilla.firefox";
    String CHROME = "com.android.chrome";
    String VALID_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJ2ZXIiOjEsImlzcyI6Imh0dHA6Ly9yYWluLm9rdGExLmNvbToxODAyIiwiaWF0IjoxNDQ5NjI0MDI2LCJleHAiOjE0NDk2Mjc2MjYsImp0aSI6IlVmU0lURzZCVVNfdHA3N21BTjJxIiwic2NvcGVzIjpbIm9wZW5pZCIsImVtYWlsIl0sImNsaWVudF9pZCI6InVBYXVub2ZXa2FESnh1a0NGZUJ4IiwidXNlcl9pZCI6IjAwdWlkNEJ4WHc2STZUVjRtMGczIn0.HaBu5oQxdVCIvea88HPgr2O5evqZlCT4UXH4UKhJnZ5px-ArNRqwhxXWhHJisslswjPpMkx1IgrudQIjzGYbtLF\n" +
            "jrrg2ueiU5-YfmKuJuD6O2yPWGTsV7X6i7ABT6P-t8PRz_RNbk-U1GXWIEkNnEWbPqYDAm_Ofh7iW0Y8WDA5ez1jbtMvd-oXMvJLctRiACrTMLJQ2e5HkbUFxgXQ_rFPNHJbNSUBDLqdi2rg_ND64DLRlXRY7hupNsvWGo0gF4WEUk8IZeaLjKw8UoIs-E\n" +
            "TEwJlAMcvkhoVVOsN5dPAaEKvbyvPC1hUGXb4uuThlwdD3ECJrtwgKqLqcWonNtiw";
    String VALID_ID_TOKEN = "eyJraWQiOiJYYjY1b1g0Um5KUXNLUGhiMHg3amVKdkt0MHpkQnM1ZkI2Q0stOEJzdkl" +
            "nIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIwMHVpejBodGJhSkhjVXRmVzBoNyIsIm5hbWUiOiJKb2huIER" +
            "vZSIsInZlciI6MSwiaXNzIjoiaHR0cHM6Ly9kZXYtNDg2MTc3Lm9rdGFwcmV2aWV3LmNvbS9vYXV0aDIvZG" +
            "VmYXVsdCIsImF1ZCI6IjBvYWl2OTR3dGpXN0RIdnZqMGg3IiwiaWF0IjoxNTUwNjA2NjcwLCJleHAiOjE1N" +
            "TA2MTAyNzAsImp0aSI6IklELlZmVk55RkZtVGtoYnBlMkV5cnljZkI5dUhHXzg4OC00YzZyc0x6OEJlR1ki" +
            "LCJhbXIiOlsicHdkIl0sImlkcCI6IjAwb2l2Ym9yYW1YR0ZCeXYwMGg3Iiwibm9uY2UiOiJSMnFUUkFDOWZ" +
            "JeWM1NUFqOEJQODJnIiwicHJlZmVycmVkX3VzZXJuYW1lIjoicGVyY2VwdG9yQGdtYWlsLmNvbSIsImF1dG" +
            "hfdGltZSI6MTU1MDYwNjY2NywiYXRfaGFzaCI6Ijk2bFc5bWVJUzJBdGtHcXRUOGVRSncifQ.dkh6RYvKhu" +
            "RGGUih6qCKijG4r4_HaR5_6KfpEQCRUBnWwLkPznxwMy3krZ-gWuSDtj_mFG-LY5TbGn4HnSpCTkK6LiPrg" +
            "FfHUEclU0_2oT1ygxa4IowZ9mrPMpjIeWruRsjJ88HjddJNkUOnoC2XWtaiVCezA-ngvxG5BhChF_7mQbNJ" +
            "fSBCBpXEYZY7zb26qGHXusH7MJOG5Ndd253qcSX4PeUAZTXCerEF4xHz1A9bLHDqsotq_IhAiYEiWtHVVu4" +
            "7FFX4LvCNmxTq0Z8rsnZlKyiD16vN-x4-uIQIdejEpjPcIA3VQqqllwZ3czFtHj9dnAoFjeYtSuUveH8xgQ";

    String INVALID_ID_TOKEN = "eyJzdWIiOiIwMHVpejBodGJhSkhjVXRmVzBoNyIsIm5hbWUiOiJKb2huIER" +
            "vZSIsInZlciI6MSwiaXNzIjoiaHR0cHM6Ly9kZXYtNDg2MTc3Lm9rdGFwcmV2aWV3LmNvbS9vYXV0aDIvZG" +
            "VmYXVsdCIsImF1ZCI6IjBvYWl2OTR3dGpXN0RIdnZqMGg3IiwiaWF0IjoxNTUwNjA2NjcwLCJleHAiOjE1N" +
            "TA2MTAyNzAsImp0aSI6IklELlZmVk55RkZtVGtoYnBlMkV5cnljZkI5dUhHXzg4OC00YzZyc0x6OEJlR1ki" +
            "LCJhbXIiOlsicHdkIl0sImlkcCI6IjAwb2l2Ym9yYW1YR0ZCeXYwMGg3Iiwibm9uY2UiOiJSMnFUUkFDOWZ" +
            "JeWM1NUFqOEJQODJnIiwicHJlZmVycmVkX3VzZXJuYW1lIjoicGVyY2VwdG9yQGdtYWlsLmNvbSIsImF1dG" +
            "hfdGltZSI6MTU1MDYwNjY2NywiYXRfaGFzaCI6Ijk2bFc5bWVJUzJBdGtHcXRUOGVRSncifQ.dkh6RYvKhu" +
            "RGGUih6qCKijG4r4_HaR5_6KfpEQCRUBnWwLkPznxwMy3krZ-gWuSDtj_mFG-LY5TbGn4HnSpCTkK6LiPrg" +
            "FfHUEclU0_2oT1ygxa4IowZ9mrPMpjIeWruRsjJ88HjddJNkUOnoC2XWtaiVCezA-ngvxG5BhChF_7mQbNJ" +
            "fSBCBpXEYZY7zb26qGHXusH7MJOG5Ndd253qcSX4PeUAZTXCerEF4xHz1A9bLHDqsotq_IhAiYEiWtHVVu4" +
            "7FFX4LvCNmxTq0Z8rsnZlKyiD16vN-x4-uIQIdejEpjPcIA3VQqqllwZ3czFtHj9dnAoFjeYtSuUveH8xgQ";

    String TOKEN_SUCCESS = "{\n" +
            "    \"access_token\" : \"eyJhbGciOiJSUzI1NiJ9.eyJ2ZXIiOjEsImlzcyI6Imh0dHA6Ly9yYWluLm9rdGExLmNvbToxODAyIiwiaWF0IjoxNDQ5Nj\n" +
            "                      I0MDI2LCJleHAiOjE0NDk2Mjc2MjYsImp0aSI6IlVmU0lURzZCVVNfdHA3N21BTjJxIiwic2NvcGVzIjpbIm9wZW5pZCIsI\n" +
            "                      mVtYWlsIl0sImNsaWVudF9pZCI6InVBYXVub2ZXa2FESnh1a0NGZUJ4IiwidXNlcl9pZCI6IjAwdWlkNEJ4WHc2STZUVjRt\n" +
            "                      MGczIn0.HaBu5oQxdVCIvea88HPgr2O5evqZlCT4UXH4UKhJnZ5px-ArNRqwhxXWhHJisslswjPpMkx1IgrudQIjzGYbtLF\n" +
            "                      jrrg2ueiU5-YfmKuJuD6O2yPWGTsV7X6i7ABT6P-t8PRz_RNbk-U1GXWIEkNnEWbPqYDAm_Ofh7iW0Y8WDA5ez1jbtMvd-o\n" +
            "                      XMvJLctRiACrTMLJQ2e5HkbUFxgXQ_rFPNHJbNSUBDLqdi2rg_ND64DLRlXRY7hupNsvWGo0gF4WEUk8IZeaLjKw8UoIs-E\n" +
            "                      TEwJlAMcvkhoVVOsN5dPAaEKvbyvPC1hUGXb4uuThlwdD3ECJrtwgKqLqcWonNtiw\",\n" +
            "    \"token_type\" : \"Bearer\",\n" +
            "    \"expires_in\" : 3600,\n" +
            "    \"scope\"      : \"openid email profile\",\n" +
            "    \"refresh_token\" : \"a9VpZDRCeFh3Nkk2VdY\",\n" +
            "    \"id_token\" : \"%s\"" +
            "}";

    String PROVIDER_CONFIG_OAUTH2 = "{\n" +
            "    \"issuer\": \"https://dev-486177.oktapreview.com/oauth2/default/\",\n" +
            "    \"authorization_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/authorize\",\n" +
            "    \"token_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/token\",\n" +
            "    \"registration_endpoint\": \"https://{baseUrl}/clients\",\n" +
            "    \"jwks_uri\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/keys\",\n" +
            "    \"response_types_supported\": [\n" +
            "        \"code\",\n" +
            "        \"token\",\n" +
            "        \"id_token\",\n" +
            "        \"code id_token\",\n" +
            "        \"code token\",\n" +
            "        \"id_token token\",\n" +
            "        \"code id_token token\"\n" +
            "    ],\n" +
            "    \"response_modes_supported\": [\n" +
            "        \"query\",\n" +
            "        \"fragment\",\n" +
            "        \"form_post\",\n" +
            "        \"okta_post_message\"\n" +
            "    ],\n" +
            "    \"grant_types_supported\": [\n" +
            "        \"authorization_code\",\n" +
            "        \"implicit\",\n" +
            "        \"refresh_token\",\n" +
            "        \"password\",\n" +
            "        \"client_credentials\"\n" +
            "    ],\n" +
            "    \"subject_types_supported\": [\n" +
            "        \"public\"\n" +
            "    ],\n" +
            "    \"scopes_supported\": [\n" +
            "        \"offline_access\",\n" +
            "    ],\n" +
            "    \"token_endpoint_auth_methods_supported\": [\n" +
            "        \"client_secret_basic\",\n" +
            "        \"client_secret_post\",\n" +
            "        \"client_secret_jwt\",\n" +
            "        \"none\"\n" +
            "    ],\n" +
            "    \"claims_supported\": [\n" +
            "       \"ver\",\n" +
            "       \"jti\",\n" +
            "       \"iss\",\n" +
            "       \"aud\",\n" +
            "       \"iat\",\n" +
            "       \"exp\",\n" +
            "       \"cid\",\n" +
            "       \"uid\",\n" +
            "       \"scp\",\n" +
            "       \"sub\"\n" +
            "  ],\n" +
            "    \"code_challenge_methods_supported\": [\n" +
            "        \"S256\"\n" +
            "    ],\n" +
            "    \"introspection_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/introspect\",\n" +
            "    \"introspection_endpoint_auth_methods_supported\": [\n" +
            "        \"client_secret_basic\",\n" +
            "        \"client_secret_post\",\n" +
            "        \"client_secret_jwt\",\n" +
            "        \"none\"\n" +
            "    ],\n" +
            "    \"revocation_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/revoke\",\n" +
            "    \"revocation_endpoint_auth_methods_supported\": [\n" +
            "        \"client_secret_basic\",\n" +
            "        \"client_secret_post\",\n" +
            "        \"client_secret_jwt\",\n" +
            "        \"none\"\n" +
            "    ],\n" +
            "    \"end_session_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/logout\",\n" +
            "    \"request_parameter_supported\": true,\n" +
            "    \"request_object_signing_alg_values_supported\": [\n" +
            "        \"HS256\",\n" +
            "        \"HS384\",\n" +
            "        \"HS512\"\n" +
            "    ]\n" +
            "}";

    String PROVIDER_CONFIG = "{\n" +
            "    \"issuer\": \"https://dev-486177.oktapreview.com/oauth2/default\",\n" +
            "    \"authorization_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/authorize\",\n" +
            "    \"token_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/token\",\n" +
            "    \"userinfo_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/userinfo\",\n" +
            "    \"registration_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/v1/clients/0oaiv94wtjW7DHvvj0h7\",\n" +
            "    \"jwks_uri\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/keys?client_id=0oaiv94wtjW7DHvvj0h7\",\n" +
            "    \"response_types_supported\": [\n" +
            "        \"code\"\n" +
            "    ],\n" +
            "    \"response_modes_supported\": [\n" +
            "        \"query\",\n" +
            "        \"fragment\",\n" +
            "        \"form_post\",\n" +
            "        \"okta_post_message\"\n" +
            "    ],\n" +
            "    \"grant_types_supported\": [\n" +
            "        \"refresh_token\",\n" +
            "        \"authorization_code\"\n" +
            "    ],\n" +
            "    \"subject_types_supported\": [\n" +
            "        \"public\"\n" +
            "    ],\n" +
            "    \"id_token_signing_alg_values_supported\": [\n" +
            "        \"RS256\"\n" +
            "    ],\n" +
            "    \"scopes_supported\": [\n" +
            "        \"openid\",\n" +
            "        \"profile\",\n" +
            "        \"email\",\n" +
            "        \"address\",\n" +
            "        \"phone\",\n" +
            "        \"offline_access\"\n" +
            "    ],\n" +
            "    \"token_endpoint_auth_methods_supported\": [\n" +
            "        \"none\"\n" +
            "    ],\n" +
            "    \"claims_supported\": [\n" +
            "        \"iss\",\n" +
            "        \"ver\",\n" +
            "        \"sub\",\n" +
            "        \"aud\",\n" +
            "        \"iat\",\n" +
            "        \"exp\",\n" +
            "        \"jti\",\n" +
            "        \"auth_time\",\n" +
            "        \"amr\",\n" +
            "        \"idp\",\n" +
            "        \"nonce\",\n" +
            "        \"name\",\n" +
            "        \"nickname\",\n" +
            "        \"preferred_username\",\n" +
            "        \"given_name\",\n" +
            "        \"middle_name\",\n" +
            "        \"family_name\",\n" +
            "        \"email\",\n" +
            "        \"email_verified\",\n" +
            "        \"profile\",\n" +
            "        \"zoneinfo\",\n" +
            "        \"locale\",\n" +
            "        \"address\",\n" +
            "        \"phone_number\",\n" +
            "        \"picture\",\n" +
            "        \"website\",\n" +
            "        \"gender\",\n" +
            "        \"birthdate\",\n" +
            "        \"updated_at\",\n" +
            "        \"at_hash\",\n" +
            "        \"c_hash\"\n" +
            "    ],\n" +
            "    \"code_challenge_methods_supported\": [\n" +
            "        \"S256\"\n" +
            "    ],\n" +
            "    \"introspection_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/introspect\",\n" +
            "    \"introspection_endpoint_auth_methods_supported\": [\n" +
            "        \"none\"\n" +
            "    ],\n" +
            "    \"revocation_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/revoke\",\n" +
            "    \"revocation_endpoint_auth_methods_supported\": [\n" +
            "        \"none\"\n" +
            "    ],\n" +
            "    \"end_session_endpoint\": \"https://dev-486177.oktapreview.com/oauth2/default/v1/logout\",\n" +
            "    \"request_parameter_supported\": false\n" +
            "}";

    String USER_PROFILE = "{\n  \"sub\": \"00uid4BxXw6I6TV4m0g3\",\n  \"name\" :\"John Doe\",\n" +
            " \"nickname\":\"Jimmy\",\n  \"given_name\":\"John\",\n " +
            " \"middle_name\":\"James\",\n" +
            " \"family_name\":\"Doe\",\n  \"profile\":\"https://example.com/john.doe\",\n" +
            " \"zoneinfo\":\"America/Los_Angeles\",\n  \"locale\":\"en-US\",\n" +
            " \"updated_at\":1311280970,\n  \"email\":\"john.doe@example.com\",\n" +
            " \"email_verified\":true,\n" +
            " \"address\" : { \"street_address\":\"123 Hollywood Blvd.\"," +
            " \"locality\":\"Los Angeles\", \"region\":\"CA\", \"postal_code\":\"90210\"," +
            " \"country\":\"US\" },\n  \"phone_number\":\"+1 (425) 555-1212\"\n}";

    String TOKEN_RESPONSE = "{ \"access_token\" : " +
            "\"ACCESS_TOKEN\",\n\"token_type\" : " +
            "\"Bearer\",\n \"expires_in\" : 3600,\n " +
            "\"scope\" : \"openid profile offline_access\",\n " +
            "\"refresh_token\" : \"REFRESH_TOKEN\",\n\"id_token\" : \"ID_TOKEN\"\n}";

    String INVALID_CLIENT = "{\n" +
            "  \"error\": \"invalid_client\",\n" +
            "  \"error_description\": \"No client credentials found.\"\n" +
            "}";

    String WWW_AUTHENTICATE = "WWW-Authenticate";

    String UNAUTHORIZED_INVALID_TOKEN = "Bearer authorization_uri=" +
            "\"http://samples-test.oktapreview.com/oauth2/v1/authorize\", " +
            "realm=\"http://samples-test.oktapreview.com\", scope=\"openid\", " +
            "error=\"invalid_token\", error_description=\"The access token has been revoked.\", " +
            "resource=\"/oauth2/v1/userinfo\"";

    String FORBIDDEN = "Bearer error=\"insufficient_scope\", " +
            "error_description=\"The access token must provide access to at " +
            "least one of these scopes - profile, email, address or phone\"";

    String CONFIGURATION_NOT_FOUND = "{\n" +
            "    \"errorCode\": \"E0000007\",\n" +
            "    \"errorSummary\": \"Not found: Resource not found: authServerId AuthorizationServer\",\n" +
            "    \"errorLink\": \"E0000007\",\n" +
            "    \"errorId\": \"oaeQdc5IvrlSGGnewf-cqqDqA\",\n" +
            "    \"errorCauses\": not found\n" +
            "}";

    String INTROSPECT_RESPONSE = "{\n" +
            "    \"active\" : true,\n" +
            "    \"token_type\" : \"Bearer\",\n" +
            "    \"scope\" : \"openid profile\",\n" +
            "    \"client_id\" : \"a9VpZDRCeFh3Nkk2VdYa\",\n" +
            "    \"username\" : \"john.doe@example.com\",\n" +
            "    \"exp\" : 1451606400,\n" +
            "    \"iat\" : 1451602800,\n" +
            "    \"sub\" : \"john.doe@example.com\",\n" +
            "    \"aud\" : \"https://{yourOktaDomain}\",\n" +
            "    \"iss\" : \"https://{yourOktaDomain}/oauth2/orsmsg0aWLdnF3spV0g3\",\n" +
            "    \"jti\" : \"AT.7P4KlczBYVcWLkxduEuKeZfeiNYkZIC9uGJ28Cc-YaI\",\n" +
            "    \"uid\" : \"00uid4BxXw6I6TV4m0g3\"\n" +
            "}";
}
