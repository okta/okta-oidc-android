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
package com.okta.oidc.net.response;

/**
 * The response from the {@code /introspect} endpoint.
 * Based on the type of token and whether it is active, the returned response
 * contains a different set of information.
 *
 * @see "Introspect Response Properties
 * <https://developer.okta.com/docs/api/resources/oidc/#introspect>"
 */
public final class IntrospectResponse {
    /**
     * Indicates whether the token is active or not.
     */
    public boolean active;
    /**
     * The type of token. The value is always Bearer.
     */
    public String token_type;
    /**
     * A space-delimited list of scopes.
     */
    public String scope;
    /**
     * The ID of the client associated with the token.
     */
    public String client_id;
    /**
     * The ID of the device associated with the token
     */
    public String device_id;
    /**
     * The username associated with the token.
     */
    public String username;
    /**
     * Identifies the time (a timestamp in seconds since January 1, 1970 UTC)
     * before which the token must not be accepted for processing.
     */
    public int nbf;
    /**
     * The expiration time of the token in seconds since January 1, 1970 UTC.
     */
    public int exp;
    /**
     * The issuing time of the token in seconds since January 1, 1970 UTC.
     */
    public int iat;
    /**
     * The subject of the token.
     */
    public String sub;
    /**
     * The audience of the token.
     */
    public String aud;
    /**
     * The issuer of the token.
     */
    public String iss;
    /**
     * The identifier of the token.
     */
    public String jti;
    /**
     * The user ID. This parameter is returned only if the token is an access token and the subject is an end user.
     */
    public String uid;
}
