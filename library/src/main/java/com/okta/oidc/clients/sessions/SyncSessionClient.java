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
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * This is the client for Okta OpenID Connect & OAuth 2.0 APIs. You can get the client when
 * user is authorized. These are synchronous methods.
 * For asynchronous methods @see {@link SessionClient}
 *
 * <pre>
 * {@code
 * //create the configuration
 * OIDCConfig config = new OIDCConfig.Builder()
 *     .clientId("{clientId}")
 *     .redirectUri("{redirectUri}")
 *     .endSessionRedirectUri("{endSessionUri}")
 *     .scopes("openid", "profile", "offline_access")
 *     .discoveryUri("https://{yourOktaDomain}")
 *     .create();
 *
 * SyncWebAuthClient auth = new Okta.SyncWebAuthBuilder()
 *     .withConfig(config)
 *     .withContext(getApplicationContext())
 *     .withStorage(new SharedPreferenceStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .withCallbackExecutor(Executors.newSingleThreadExecutor())
 *     .supportedBrowsers(CHROME_PACKAGE_ID, FIREFOX_PACKAGE_ID)
 *     .create();
 *
 * SessionClient client = auth.getSessionClient();
 * }
 * </pre>
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
public interface SyncSessionClient extends BaseSessionClient {
    /**
     * Performs a custom authorized request with the access token automatically added to the
     * "Authorization" header with the standard OAuth 2.0 prefix of "Bearer".
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     JSONObject result =
     *         client.authorizedRequest(uri, properties, postParameters,
     *                                  ConnectionParameters.RequestMethod.POST);
     *     //handle results
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     *
     * </pre>
     * }
     *
     * @param uri            the uri to protected resource
     * @param properties     the query request properties
     * @param postParameters the post parameters
     * @param method         the http method {@link ConnectionParameters.RequestMethod}
     * @return the JSONObject result
     * @throws AuthorizationException the authorization exception
     */
    JSONObject authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                 @Nullable Map<String, String> postParameters,
                                 @NonNull ConnectionParameters.RequestMethod method)
            throws AuthorizationException;

    /**
     * Performs a custom authorized request with the access token automatically added to the
     * "Authorization" header with the standard OAuth 2.0 prefix of "Bearer".
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     ByteBuffer result =
     *         client.authorizedRequest(uri, properties, null, requestBody,
     *                                  ConnectionParameters.RequestMethod.POST);
     *     //handle results
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     *
     * </pre>
     * }
     *
     * @param uri            the uri to protected resource
     * @param properties     the query request properties
     * @param postParameters the post parameters
     * @param requestBody    the post request body
     * @param method         the http method {@link ConnectionParameters.RequestMethod}
     * @return the ByteBuffer result
     * @throws AuthorizationException the authorization exception
     */
    ByteBuffer authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                 @Nullable Map<String, String> postParameters,
                                 @Nullable byte[] requestBody,
                                 @NonNull ConnectionParameters.RequestMethod method)
            throws AuthorizationException;

    /**
     * Get user profile returns any claims for the currently logged-in user.
     *
     * <p>This must be done after the user is logged-in (client has a valid access token).
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     UserInfo info = client.getUserProfile();
     *     //handle results
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     * </pre>
     * }
     *
     * @return the user info
     * @throws AuthorizationException the authorization exception
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#userinfo">User info</a>
     */
    UserInfo getUserProfile() throws AuthorizationException;

    /**
     * Introspect token takes an access, refresh, or ID token, and returns a boolean
     * indicating whether it is active or not. If the token is active, additional data about
     * the token is also returned {@link IntrospectInfo}. If the token is invalid, expired,
     * or revoked, it is considered inactive.
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     IntrospectInfo info = client.introspectToken(client.getTokens().getRefreshToken(),
     *                                                  TokenTypeHint.REFRESH_TOKEN);
     *     //handle results
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     * </pre>
     * }
     *
     * @param token     the token
     * @param tokenType the token type
     * @return the introspect info {@link IntrospectInfo}
     * @throws AuthorizationException the authorization exception
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#introspect">Introspect token</a>
     */
    IntrospectInfo introspectToken(String token, String tokenType) throws AuthorizationException;

    /**
     * Revoke token takes an access or refresh token and revokes it. Revoked tokens are considered
     * inactive at the introspection endpoint. A client may only revoke its own tokens.
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     boolean success = client.revokeToken(client.getTokens().getRefreshToken());
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     * </pre>
     * }
     *
     * @param token the token to be revoked
     * @return true if token was revoked
     * @throws AuthorizationException the authorization exception
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#revoke">Revoke token</a>
     */
    Boolean revokeToken(String token) throws AuthorizationException;

    /**
     * Refresh token returns access, refresh, and ID tokens {@link Tokens}.
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     Tokens token = client.refreshToken();
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     * </pre>
     * }
     *
     * @return the tokens
     * @throws AuthorizationException the authorization exception
     */
    Tokens refreshToken() throws AuthorizationException;

    /**
     * Gets tokens {@link Tokens}.
     * Example usage:
     * {@code
     * <pre>
     * try {
     *     Tokens token = getTokens();
     * } catch (AuthorizationException ex) {
     *     //handle exception
     * }
     * </pre>
     * }
     *
     * @return the tokens
     * @throws AuthorizationException the authorization exception
     */
    Tokens getTokens() throws AuthorizationException;

    /**
     * Clears all data. This will remove all tokens from the client.
     */
    void clear();

    /**
     * Attempt to cancel the current api request. Does not guarantee that the current call
     * will not finish.
     */
    void cancel();
}
