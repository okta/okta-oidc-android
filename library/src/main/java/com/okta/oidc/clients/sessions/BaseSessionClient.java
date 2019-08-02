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

import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

interface BaseSessionClient {
    /**
     * Checks to see if the user is authenticated. If the client have a access or ID token then
     * the user is considered authenticated and this call will return true. This does not check the
     * validity of the access token which could be expired or revoked.
     *
     * @return the boolean
     */
    boolean isAuthenticated();

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
     * Remove all tokens from storage.
     */
    void removeAllTokens();

    /**
     * Remove access token from storage.
     *
     * @return true if successfully removed. false otherwise.
     */
    boolean removeAccessToken();

    /**
     * Remove refresh token from storage.
     *
     * @return true if successfully removed. false otherwise.
     */
    boolean removeRefreshToken();

    /**
     * Remove ID token from storage.
     *
     * @return true if successfully removed. false otherwise.
     */
    boolean removeIdToken();
}
