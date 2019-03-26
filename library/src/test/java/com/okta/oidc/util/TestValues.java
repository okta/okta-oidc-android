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

import android.net.Uri;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;

public class TestValues {
    public static final String CUSTOM_STATE = "CUSTOM_STATE";
    public static final String LOGIN_HINT = "LOGIN_HINT";
    public static final String CLIENT_ID = "CLIENT_ID";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String ID_TOKEN = "VALID_ID_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    public static OIDCAccount getAccountWithUrl(String url) {
        return new OIDCAccount.Builder()
                .clientId(CLIENT_ID)
                .redirectUri(url + "callback")
                .endSessionRedirectUri(url + "logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri(url)
                .create();
    }

    public static AuthorizeRequest getAuthorizeRequest(OIDCAccount account,
                                                       String nonce, String state) {
        return new AuthorizeRequest.Builder().codeVerifier("verifier")
                .authorizeEndpoint(account.getDiscoveryUri().toString())
                .redirectUri(account.getRedirectUri().toString())
                .scope("openid", "email", "profile")
                .nonce(nonce)
                .state(state)
                .create();
    }

    public static AuthorizeResponse getAuthorizeResponse(String state, String code) {
        String uri = String.format("com.okta.test:/callback?code=%s&state=%s", code, state);
        return AuthorizeResponse.fromUri(Uri.parse(uri));
    }
}
