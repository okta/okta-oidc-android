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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.gson.Gson;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.storage.Persistable;

/**
 * @hide
 */
@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TokenResponse implements Persistable {
    private static final int THOUSAND = 1000;
    private String access_token;
    private String token_type;
    private String expires_in;
    private String scope;
    private String refresh_token;
    private String id_token;
    private long expiresAt = -1;

    @Nullable
    public String getAccessToken() {
        return access_token;
    }

    @NonNull
    public String getTokenType() {
        return token_type;
    }

    @NonNull
    public String getExpiresIn() {
        return expires_in;
    }

    @NonNull
    public String getScope() {
        return scope;
    }

    @Nullable
    public String getRefreshToken() {
        return refresh_token;
    }

    @Nullable
    public String getIdToken() {
        return id_token;
    }

    public void removeToken(String tokenTypeHint) {
        if (TokenTypeHint.ACCESS_TOKEN.equals(tokenTypeHint)) {
            access_token = null;
            expiresAt = -1;
        } else if (TokenTypeHint.ID_TOKEN.equals(tokenTypeHint)) {
            id_token = null;
        } else if (TokenTypeHint.REFRESH_TOKEN.equals(tokenTypeHint)) {
            refresh_token = null;
        }
    }

    public TokenResponse() {
        //NO-OP
    }

    //only called from token request
    public void setCreationTime(long creationTime) {
        if (expiresAt < 0) {
            expiresAt = System.currentTimeMillis();
        }
    }

    public long getExpiresAt() {
        if (expiresAt > 0) {
            expiresAt += Integer.parseInt(expires_in) * THOUSAND;
        }
        return expiresAt;
    }

    public static final Persistable.Restore<TokenResponse> RESTORE =
            new Persistable.Restore<TokenResponse>() {
                private static final String KEY = "TokenResponse";

                @NonNull
                @Override
                public String getKey() {
                    return KEY;
                }

                @Override
                public TokenResponse restore(@Nullable String data) {
                    if (data != null) {
                        return new Gson().fromJson(data, TokenResponse.class);
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
