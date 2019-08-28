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

package com.okta.oidc;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * An access token is a JSON web token (JWT) encoded in Base64 URL-encoded format that contains a
 * header, payload, and signature. A resource server can authorize the client to access particular
 * resources based on the scopes and claims in the access token. This is an Okta specific access
 * token.
 *
 * <p>NOTE: Use of the access token differs depending on whether you are using the Okta Org
 * Authorization Server or a Custom Authorization Server. While the structure of an access token
 * retrieved from a Custom Authorization Server is guaranteed to not change, the structure of the
 * access token issued by the Okta Org Authorization Server is subject to change.
 *
 * @see "Access Token <https://developer.okta.com/docs/reference/api/oidc/#access-token>"
 */
@SuppressWarnings("unused")
public class OktaAccessToken {
    private static final int NUMBER_OF_SECTIONS = 3;

    /**
     * The header of a access token.
     * {@link Header}
     */
    @VisibleForTesting
    Header mHeader;

    /**
     * The claims section of a access token.
     * {@link Payload}
     */
    @VisibleForTesting
    Payload mPayload;

    /**
     * The signature of a access token.
     */
    @VisibleForTesting
    String mSignature;

    /**
     * Claims in the Header Section.
     *
     * @see "Access Token Header <https://developer.okta.com/docs/reference/api/oidc/#access-token-header>"
     */
    public static class Header {
        /**
         * Identifies the digital signature algorithm used. This is always RS256.
         */
        public String alg;
        /**
         * Identifies the public-key used to sign the access_token. The corresponding public-key can
         * be found via the JWKS in the discovery document.
         */
        public String kid;
    }

    /**
     * Payload.
     *
     * @see "Access token payload <https://developer.okta.com/docs/reference/api/oidc/#access-token-payload>"
     */
    public static class Payload {
        /**
         * Identifies the audience (resource URI or server) that this access token is intended for.
         */
        public List<String> aud;
        /**
         * Client ID of the client that requested the access token.
         */
        public String cid;
        /**
         * The time the access token expires, represented in Unix time (seconds).
         */
        public int exp;
        /**
         * The time the access token was issued, represented in Unix time (seconds).
         */
        public int iat;
        /**
         * The Issuer Identifier of the response. This value is the unique identifier for the
         * Authorization Server instance.
         */
        public String iss;
        /**
         * A unique identifier for this access token for debugging and revocation purposes.
         */
        public String jti;
        /**
         * List of scopes that are granted to this access token.
         */
        public List<String> scp;
        /**
         * The subject. A name for the user or a unique identifier for the client.
         */
        public String sub;
        /**
         * A unique identifier for the user. It isn't included in the access token if there is no
         * user bound to it.
         */
        public String uid;
        /**
         * The semantic version of the access token.
         */
        public int ver;
        /**
         * Custom claims.
         */
        public String custom_claim;
    }

    private OktaAccessToken(Header header, Payload payload, String signature) {
        mHeader = header;
        mPayload = payload;
        mSignature = signature;
    }

    /**
     * Get the header claims. {@link Header}
     *
     * @return the header
     */
    public Header getHeader() {
        return mHeader;
    }

    /**
     * Get the payload. {@link Payload}
     *
     * @return the payload
     */
    public Payload getPayload() {
        return mPayload;
    }

    /**
     * Get the signature.
     *
     * @return the signature
     * @see "Access Token Signature <https://developer.okta.com/docs/reference/api/oidc/#access-token-signature>"
     */
    public String getSignature() {
        return mSignature;
    }

    /**
     * Parses a JSON Web Token (JWT).
     *
     * @param token the based64 encoded AccessToken
     * @return the okta access token
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static OktaAccessToken parseAccessToken(@NonNull String token)
            throws IllegalArgumentException {
        String[] sections = token.split("\\.");
        if (sections.length < NUMBER_OF_SECTIONS) {
            throw new IllegalArgumentException("token missing header, payload or" +
                    " signature section");
        }
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(OktaIdToken.ArrayTypeAdapter.CREATE).create();
        //decode header
        String headerSection = new String(Base64.decode(sections[0], Base64.URL_SAFE));
        Header header = gson.fromJson(headerSection, Header.class);
        //decode payload
        String payloadSection = new String(Base64.decode(sections[1], Base64.URL_SAFE));
        Payload payload = gson.fromJson(payloadSection, Payload.class);
        String signature = new String(Base64.decode(sections[2], Base64.URL_SAFE));
        return new OktaAccessToken(header, payload, signature);
    }
}
