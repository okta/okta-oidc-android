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

import android.support.annotation.Nullable;

/**
 * OpenId tokens.
 */
public class Tokens {

    private String mIdToken;
    private String mAccessToken;
    private String mRefreshToken;

    public Tokens(String idToken, String accessToken, String refreshToken) {
        this.mIdToken = idToken;
        this.mAccessToken = accessToken;
        this.mRefreshToken = refreshToken;
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
}
