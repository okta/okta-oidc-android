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

import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;

/**
 * This is the client for Okta OpenID Connect & OAuth 2.0 APIs. You can get the client when
 * user is authorized. These are asynchronous methods.
 * For synchronous methods @see {@link SyncSessionClient}
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
 * WebAuthClient auth = new Okta.WebBuilder()
 *     .withConfig(config)
 *     .withContext(getApplicationContext())
 *     .withStorage(new SimpleOktaStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .withCallbackExecutor(Executors.newSingleThreadExecutor())
 *     .supportedBrowsers(CHROME_PACKAGE_ID, FIREFOX_PACKAGE_ID)
 *     .create();
 *
 * SessionClient client = auth.getSessionClient();
 * }
 * </pre>
 * <p>
 * Note that callbacks are executed on the UI thread unless a executor is provided to the builder.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
public interface SessionClient {
    /**
     * Performs a custom authorized request with the access token automatically added to the
     * "Authorization" header with the standard OAuth 2.0 prefix of "Bearer".
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.authorizedRequest(uri, properties,
     *                 postParameters, HttpConnection.RequestMethod.POST,
     *                 new RequestCallback<JSONObject, AuthorizationException>() {
     *     @Override
     *     public void onSuccess(@NonNull JSONObject result) {
     *         //handle JSONObject result.
     *     }
     *
     *     @Override
     *     public void onError(String error, AuthorizationException exception) {
     *         //handle failed request
     *     }
     * });
     * </pre>
     * }
     *
     * @param uri            the uri to protected resource
     * @param properties     the query request properties
     * @param postParameters the post parameters
     * @param method         the http method {@link HttpConnection.RequestMethod}
     * @param cb             the RequestCallback to be executed when request is finished.
     */
    void authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                           @Nullable Map<String, String> postParameters,
                           @NonNull HttpConnection.RequestMethod method,
                           final RequestCallback<JSONObject, AuthorizationException> cb);

    /**
     * Get user profile returns any claims for the currently logged-in user.
     * <p>
     * This must be done after the user is logged-in (client has a valid access token).
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.getUserProfile(new RequestCallback<JSONObject, AuthorizationException>() {
     *     @Override
     *     public void onSuccess(@NonNull JSONObject result) {
     *         //handle JSONObject result.
     *     }
     *
     *     @Override
     *     public void onError(String error, AuthorizationException exception) {
     *         //handle failed userinfo request
     *     }
     * });
     *
     * </pre>
     * }
     *
     * @param cb the RequestCallback to be executed when request is finished.
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#userinfo">User info</a>
     */
    void getUserProfile(final RequestCallback<UserInfo, AuthorizationException> cb);

    /**
     * Introspect token takes an access, refresh, or ID token, and returns a boolean
     * indicating whether it is active or not. If the token is active, additional data about
     * the token is also returned {@link IntrospectInfo}. If the token is invalid, expired,
     * or revoked, it is considered inactive.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.introspectToken(client.getTokens().getRefreshToken(),
     *     TokenTypeHint.REFRESH_TOKEN, new RequestCallback<IntrospectResponse, AuthorizationException>() {
     *         @Override
     *         public void onSuccess(@NonNull IntrospectResponse result) {
     *             //handle introspect response.
     *         }
     *
     *         @Override
     *         public void onError(String error, AuthorizationException exception) {
     *             //handle request error
     *         }
     *     }
     * );
     * </pre>
     * }
     *
     * @param token     for introspection. Can be the access, refresh or ID token.
     * @param tokenType the type must be of {@link com.okta.oidc.net.params.TokenTypeHint}
     * @param cb        the RequestCallback to be executed when request is finished.
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#introspect">Introspect token</a>
     */
    void introspectToken(String token, String tokenType,
                         final RequestCallback<IntrospectInfo, AuthorizationException> cb);

    /**
     * Revoke token takes an access or refresh token and revokes it. Revoked tokens are considered
     * inactive at the introspection endpoint. A client may only revoke its own tokens.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.revokeToken(client.getTokens().getRefreshToken(),
     *     new RequestCallback<Boolean, AuthorizationException>() {
     *         @Override
     *         public void onSuccess(@NonNull Boolean result) {
     *             //handle result
     *         }
     *         @Override
     *         public void onError(String error, AuthorizationException exception) {
     *             //handle request error
     *         }
     *     });
     * </pre>
     * }
     *
     * @param token the token to be revoked. Can be the access or refresh token.
     * @param cb    the RequestCallback to be executed when request is finished.
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#revoke">Revoke token</a>
     */
    void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb);

    /**
     * Refresh token returns access, refresh, and ID tokens {@link Tokens}.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
     *     @Override
     *     public void onSuccess(@NonNull Tokens result) {
     *         //handle success.
     *     }
     *
     *     @Override
     *     public void onError(String error, AuthorizationException exception) {
     *         //handle request failure
     *     }
     * });
     * </pre>
     * }
     *
     * @param cb the RequestCallback to be executed when request is finished.
     */
    void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb);

    /**
     * Gets tokens {@link Tokens}.
     *
     * @return the tokens
     */
    Tokens getTokens();


    /**
     * Checks to see if the user is logged-in. If the client have a access or ID token then
     * the user is considered logged-in. This does not check the validity of the access token which
     * could be expired or revoked. For more information about the tokens see
     * {@link #introspectToken(String, String, RequestCallback) introspectToken}
     *
     * @return the boolean
     */
    boolean isLoggedIn();

    /**
     * Clears all data. This will remove all tokens from the client.
     */
    void clear();
}
