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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.oidc.net.response.TokenResponse;

/**
 * OpenId tokens.
 */
public class Tokens {

    private String mIdToken;
    private String mAccessToken;
    private String mRefreshToken;
    private int mExpiresIn;
    private String[] mScope;

    /**
     * Instantiates a new Tokens.
     *
     * @param idToken      the id token
     * @param accessToken  the access token
     * @param refreshToken the refresh token
     * @param expiresIn    the expires in
     * @param scope        the scope
     */
    Tokens(String idToken, String accessToken, String refreshToken, int expiresIn, String[] scope) {
        mIdToken = idToken;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mExpiresIn = expiresIn;
        mScope = scope;
    }

    /**
     * Instantiates a new Tokens from a TokenResponse.
     *
     * @param response the TokenResponse
     */
    public Tokens(@NonNull TokenResponse response) {
        this(response.getIdToken(), response.getAccessToken(),
                response.getRefreshToken(), Integer.parseInt(response.getExpiresIn()),
                response.getScope().split(" "));
    }

    /**
     * The current ID token, if available.
     * This is a base64 encoded string. For getting a OktaIdToken use
     * {@link OktaIdToken#parseIdToken(String)}
     *
     * @return id token.
     */
    @Nullable
    public String getIdToken() {
        return mIdToken;
    }

    /**
     * The current access token, if available.
     *
     * @return access token
     */
    @Nullable
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * The most recent refresh token received from the server, if available.
     *
     * @return refresh token.
     */
    @Nullable
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * The time in seconds when tokens expired.
     *
     * @return expiration of token.
     */
    public int getExpiresIn() {
        return mExpiresIn;
    }

    /**
     * List of scopes.
     *
     * @return scopes. string [ ]
     */
    @Nullable
    public String[] getScope() {
        return mScope;
    }

}
