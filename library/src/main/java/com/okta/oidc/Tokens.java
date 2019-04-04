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

import com.okta.oidc.net.response.TokenResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * OpenId tokens.
 */
public class Tokens {

    private String mIdToken;
    private String mAccessToken;
    private String mRefreshToken;
    private int mExpiresIn;
    private String[] mScope;

    public Tokens(String idToken, String accessToken, String refreshToken, String expiresIn,
                  String scope) {
        this.mIdToken = idToken;
        this.mAccessToken = accessToken;
        this.mRefreshToken = refreshToken;
        if (expiresIn != null) {
            this.mExpiresIn = Integer.parseInt(expiresIn);
        }
        this.mScope = new String[]{};
        if (scope != null) {
            this.mScope = scope.split(" ");
        }
    }

    Tokens(@NonNull TokenResponse response) {
        this(response.getIdToken(), response.getAccessToken(),
                response.getRefreshToken(), response.getExpiresIn(),
                response.getScope());
    }

    /**
     * The current ID token, if available.
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
     * @return refresh token.
     */
    @Nullable
    public int getExpiresIn() {
        return mExpiresIn;
    }

    /**
     * List of scopes
     *
     * @return refresh token.
     */
    @Nullable
    public String[] getScope() {
        return mScope;
    }

}
