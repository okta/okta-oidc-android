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

package com.okta.oidc.clients.sessions;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import java.util.Map;

public interface SyncSessionClient {
    AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                        @Nullable Map<String, String> postParameters,
                                        @NonNull HttpConnection.RequestMethod method);

    UserInfo getUserProfile() throws AuthorizationException;

    IntrospectInfo introspectToken(String token, String tokenType) throws AuthorizationException;

    Boolean revokeToken(String token) throws AuthorizationException;

    Tokens refreshToken() throws AuthorizationException;

    Tokens getTokens();

    boolean isLoggedIn();

    void clear();
}
