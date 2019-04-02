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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.okta.oidc.storage.Persistable;

public class TokenResponse implements Persistable {
    private String access_token;
    private String token_type;
    private String expires_in;
    private String scope;
    private String refresh_token;
    private String id_token;

    public String getAccessToken() {
        return access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public String getExpiresIn() {
        return expires_in;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public String getIdToken() {
        return id_token;
    }

    public boolean isLoggedIn() {
        return access_token != null || id_token != null;
    }

    public TokenResponse() {
        //NO-OP
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

                @Override
                public boolean encrypted() {
                    return true;
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

    @Override
    public boolean encrypt() {
        return RESTORE.encrypted();
    }
}
