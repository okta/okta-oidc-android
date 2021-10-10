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

import androidx.annotation.Nullable;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

/**
 * The Authentication client for logging in using a sessionToken. The client calls are synchronous.
 *
 * <p>For login using web browser
 * {@link com.okta.oidc.clients.web.WebAuthClient}
 * For asynchronous client
 * {@link com.okta.oidc.clients.AuthClient}
 */
public interface SyncAuthClient extends BaseAuth<SyncSessionClient> {
    /**
     * Sign in with a session token. This is for logging in without using the implicit flow.
     * A session token can be obtained by using the AuthClient API. For more information
     * about different types of
     * <a href=https://developer.okta.com/authentication-guide/auth-overview/#choosing-an-oauth-2-0-flow>AuthClient flows</a>
     *
     * @param sessionToken the session token
     * @param payload      the {@link AuthenticationPayload payload}
     * @return the {@link Result authorizationResult}
     */
    Result signIn(String sessionToken, @Nullable AuthenticationPayload payload);

    /**
     * Sign in with a session token. This is for logging in without using the implicit flow.
     * A session token can be obtained by using the AuthClient API. For more information
     * about different types of
     * <a href=https://developer.okta.com/authentication-guide/auth-overview/#choosing-an-oauth-2-0-flow>AuthClient flows</a>
     *
     * @param deviceSecret the device secret
     * @param subjectToken the subject_token (id_token) returned with the device_secret
     *                     on /authorize request
     * @return the {@link Result authorizationResult}
     */
    Result signIn(String deviceSecret, String subjectToken);

    /**
     * Attempt to cancel the current api request. Does not guarantee that the current call
     * will not finish.
     */
    void cancel();

    /**
     * Use this method to migrate to another Encryption Manager. This method should decrypt data
     * using current EncryptionManager and encrypt with new one. All follow data will be encrypted
     * by new Encryption Manager
     *
     * @param manager new Encryption Manager
     * @throws AuthorizationException exception if migration fails.
     */
    void migrateTo(EncryptionManager manager) throws AuthorizationException;

    /**
     * Convenience method to completely sign out of application.
     * Performs the following operations in order:
     * 1. Revokes the access_token. If this fails step 3 will not be attempted.
     * 2. Revokes the refresh_token. If this fails step 3 will not be attempted.
     * 3. Removes all persistence data. Only if steps 1 & 2 succeeds.
     *
     * @return the bitwise status.
     * @see #SUCCESS
     * @see #FAILED_REVOKE_ACCESS_TOKEN
     * @see #FAILED_REVOKE_REFRESH_TOKEN
     * @see #FAILED_CLEAR_DATA
     */
    int signOut();

    /**
     * Convenience method to completely sign out of application.
     * Performs the following depending on the flags parameter:
     * {@link #REVOKE_ACCESS_TOKEN} Perform revoke access_token operation.
     * {@link #REVOKE_REFRESH_TOKEN} Perform revoke the refresh_token operation.
     * {@link #REMOVE_TOKENS} Removes all persistent data. Attempted only if revoke tokens succeeds
     * or no flag is set to revoke tokens.
     * {@link #ALL} All of the above flags. Same as calling {@link #signOut}
     *
     * @param flags the flag for the operations to perform.
     * @return the bitwise status.
     * @see #SUCCESS
     * @see #FAILED_REVOKE_ACCESS_TOKEN
     * @see #FAILED_REVOKE_REFRESH_TOKEN
     * @see #FAILED_CLEAR_DATA
     */
    int signOut(int flags);
}
