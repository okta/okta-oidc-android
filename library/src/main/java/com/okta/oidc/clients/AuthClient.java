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

package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

public interface AuthClient extends BaseAuth<SessionClient> {
    /**
     * Log in with a session token. This is for logging in without using the implicit flow.
     * A session token can be obtained by using the AuthClient API. For more information
     * about different types of
     * <a href=https://developer.okta.com/authentication-guide/auth-overview/#choosing-an-oauth-2-0-flow>AuthClient flows</a>
     *
     * @param sessionToken the session token
     * @param payload      the {@link AuthenticationPayload payload}
     * @param cb           the @{@link RequestCallback callback}
     * @see <a href=https://developer.okta.com/docs/api/resources/authn/>Revoke token</a>
     */
    void logIn(String sessionToken, AuthenticationPayload payload,
               RequestCallback<AuthorizationResult, AuthorizationException> cb);
}
