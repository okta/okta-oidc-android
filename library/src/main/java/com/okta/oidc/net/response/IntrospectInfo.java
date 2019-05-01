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

import androidx.annotation.Nullable;

/**
 * Encapsulates the response properties of the introspect endpoint.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#response-properties-3">
 * Introspect response properties</a>
 */
@SuppressWarnings("unused")
public final class IntrospectInfo {
    private boolean active;
    private String token_type;
    private String scope;
    private String client_id;
    private String device_id;
    private String username;
    private int nbf;
    private int exp;
    private int iat;
    private String sub;
    private String aud;
    private String iss;
    private String jti;
    private String uid;

    /**
     * Indicates whether the token is active or not.
     *
     * @return active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * The type of token. The value is always Bearer.
     *
     * @return the token type. Always Bearer.
     */
    public String getTokenType() {
        return token_type;
    }

    /**
     * A space-delimited list of scopes.
     *
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * The ID of the client associated with the token.
     *
     * @return the client id
     */
    public String getClientId() {
        return client_id;
    }

    /**
     * The ID of the device associated with the token.
     *
     * @return the device id
     */
    public String getDeviceId() {
        return device_id;
    }

    /**
     * The username associated with the token.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Identifies the time (a timestamp in seconds since January 1, 1970 UTC) before which the
     * token must not be accepted for processing.
     *
     * @return the nbf
     */
    public int getNbf() {
        return nbf;
    }

    /**
     * The expiration time of the token in seconds since January 1, 1970 UTC.
     *
     * @return the exp
     */
    public int getExp() {
        return exp;
    }

    /**
     * The issuing time of the token in seconds since January 1, 1970 UTC.
     *
     * @return the iat
     */
    public int getIat() {
        return iat;
    }

    /**
     * The subject of the token.
     *
     * @return the sub
     */
    public String getSub() {
        return sub;
    }

    /**
     * The audience of the token.
     *
     * @return the aud
     */
    public String getAud() {
        return aud;
    }

    /**
     * The issuer of the token.
     *
     * @return the iss
     */
    public String getIss() {
        return iss;
    }

    /**
     * The identifier of the token.
     *
     * @return the jti
     */
    public String getJti() {
        return jti;
    }

    /**
     * The user ID. This parameter is returned only if the token is an access token and the subject
     * is an end user.
     *
     * @return the uid
     */
    @Nullable
    public String getUid() {
        return uid;
    }
}
