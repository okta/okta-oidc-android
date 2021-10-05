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

/**
 * The interface Base auth.
 *
 * @param <S> the generic type of session client
 */
public interface BaseAuth<S> {

    /**
     * When set, signing out will attempt to revoke the access token.
     */
    int REVOKE_ACCESS_TOKEN = 0x00000001;
    /**
     * When set, signing out will attempt to revoke the refresh token.
     */
    int REVOKE_REFRESH_TOKEN = 0x00000002;
    /**
     * When set, signing out will attempt to revoke the device secret.
     */
    int REVOKE_DEVICE_SECRET = 0x00000020;
    /**
     * When set, signing out will attempt to remove tokens from persistent storage.
     */
    int REMOVE_TOKENS = 0x00000004;
    /**
     * When set, revokeTokens will attempt to clear the browser session. Only applicable
     * for {@link com.okta.oidc.clients.web.WebAuthClient}
     */
    int SIGN_OUT_SESSION = 0x00000008;
    /**
     * Internal use only. For performing all operations.
     */
    int ALL = SIGN_OUT_SESSION | REVOKE_ACCESS_TOKEN | REVOKE_REFRESH_TOKEN | REMOVE_TOKENS | REVOKE_DEVICE_SECRET;

    /**
     * Status returned when sign out steps have all completed.
     */
    int SUCCESS = 0x00000000;
    /**
     * Bitwise status returned when clearing browser failed.
     */
    int FAILED_CLEAR_SESSION = SIGN_OUT_SESSION;
    /**
     * Bitwise status returned when revoking access token failed.
     */
    int FAILED_REVOKE_ACCESS_TOKEN = REVOKE_ACCESS_TOKEN;
    /**
     * Bitwise status returned when revoking refresh token failed.
     */
    int FAILED_REVOKE_REFRESH_TOKEN = REVOKE_REFRESH_TOKEN;
    /**
     * Bitwise status returned when revoking refresh token failed.
     */
    int FAILED_REVOKE_DEVICE_SECRET = REVOKE_DEVICE_SECRET;
    /**
     * Bitwise status returned when clearing data failed.
     */
    int FAILED_CLEAR_DATA = REMOVE_TOKENS;
    /**
     * Bitwise status returned when retrieving token failed due to a encryption error.
     * Device needs to be authenticated.
     */
    int TOKEN_DECRYPT = 0x00000010;

    /**
     * Internal use only. Initial status is all ops have failed.
     */
    int FAILED_ALL = FAILED_CLEAR_SESSION | FAILED_REVOKE_ACCESS_TOKEN |
            FAILED_REVOKE_REFRESH_TOKEN | FAILED_CLEAR_DATA | FAILED_REVOKE_DEVICE_SECRET;

    /**
     * Gets session client.
     *
     * @return the session client
     */
    S getSessionClient();
}
