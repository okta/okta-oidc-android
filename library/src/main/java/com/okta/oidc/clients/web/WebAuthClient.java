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

package com.okta.oidc.clients.web;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.BaseAuth;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

/**
 * Client Web Authorization API for Okta OpenID Connect & OAuth 2.0 APIs.
 * Uses a chrome custom tab enabled browser as a user agent for authorization.
 * <pre>
 * {@code
 * OIDCConfig config = new OIDCConfig.Builder()
 *     .clientId("{clientId}")
 *     .redirectUri("{redirectUri}")
 *     .endSessionRedirectUri("{endSessionUri}")
 *     .scopes("openid", "profile", "offline_access")
 *     .discoveryUri("https://{yourOktaDomain}")
 *     .create();
 *
 * WebAuthClient client = new Okta.WebBuilder()
 *     .withConfig(config)
 *     .withContext(getApplicationContext())
 *     .withStorage(new SharedPreferenceStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .withCallbackExecutor(Executors.newSingleThreadExecutor())
 *     .supportedBrowsers(CHROME_PACKAGE_ID, FIREFOX_PACKAGE_ID)
 *     .create();
 * }
 * </pre>
 *
 * <p>Note that callbacks are executed on the UI thread unless a executor is provided to the
 * builder.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
public interface WebAuthClient extends BaseAuth<SessionClient> {

    /**
     * Checks to see if authentication is in progress.
     *
     * @return the boolean value of true if authentication is in progress otherwise false.
     */
    boolean isInProgress();

    /**
     * Sign in using implicit flow.
     *
     * <p>The result will be returned in the
     * {@link #registerCallback(ResultCallback, Activity)} callback
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     */
    void signIn(@NonNull Activity activity, AuthenticationPayload payload);

    /**
     * Sign out from okta. This will clear the browser session
     *
     * <p>The result will be returned in the
     * {@link #registerCallback(ResultCallback, Activity)} callback
     *
     * @param activity the activity
     */
    void signOutOfOkta(@NonNull Activity activity);

    /**
     * Register a callback for sign in and sign out result status. The callback is triggered when
     * {@link #signIn(Activity, AuthenticationPayload) signIn} or
     * {@link #signOutOfOkta(Activity)} signOutOfOkta} is completed.
     * Example usage:
     * {@code
     * <pre>
     * client.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
     *      @Override
     *      public void onSuccess(@NonNull AuthorizationStatus status) {
     *          if (status == AuthorizationStatus.AUTHORIZED) {
     *              //sign in success. client can be used to perform protected resource requests.
     *          } else if (status == AuthorizationStatus.SIGNED_OUT) {
     *              //sign out success. browser session is cleared.
     *          } else if (status == AuthorizationStatus.IN_PROGRESS) {
     *              //request in progress.
     *          }
     *      }
     *
     *      @Override
     *      public void onCancel() {
     *          //request canceled
     *      }
     *
     *      @Override
     *      public void onError(@NonNull String msg, AuthorizationException error) {
     *          //error
     *      }
     * }, this);
     * </pre>
     * }*
     *
     * @param resultCallback returns the result of sign in or sign out attempts.
     * @param activity       the activity which will receive the results.
     */
    void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException>
                                  resultCallback, Activity activity);

    /**
     * Unregister the callback.
     */
    void unregisterCallback();

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
     * Use this method to handle the onActivityResult. If using regular activity and chrome custom
     * tabs passes data back to the main activity. This must be called to parse the results of
     * the login.
     *
     * @param requestCode request code of login or logout.
     * @param resultCode  result either OK or CANCEL
     * @param data        the redirected data from chrome custom tab.
     */
    void handleActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * Convenience method to completely sign out of application.
     * Performs the following operations in order:
     * 1. Revokes the access_token. If this fails step 3 will not be attempted.
     * 2. Revokes the refresh_token. If this fails step 3 will not be attempted.
     * 3. Removes all persistence data. Only if steps 1 & 2 succeeds.
     * 4. Clears browser session only if this is a {@link com.okta.oidc.clients.web.WebAuthClient}
     * instance. Will always be attempted regardless of the other operations.
     *
     * @param activity        the activity
     * @param requestCallback the callback containing the bitwise status. AuthorizationException
     *                        may be null.
     * @see #SUCCESS
     * @see #FAILED_REVOKE_ACCESS_TOKEN
     * @see #FAILED_REVOKE_REFRESH_TOKEN
     * @see #FAILED_CLEAR_DATA
     * @see #FAILED_CLEAR_SESSION
     */
    void signOut(@NonNull Activity activity,
                 RequestCallback<Integer, AuthorizationException> requestCallback);

    /**
     * Convenience method to completely sign out of application.
     * Performs the following depending on the flags parameter:
     * {@link #REVOKE_ACCESS_TOKEN} Perform revoke access_token operation.
     * {@link #REVOKE_REFRESH_TOKEN} Perform revoke the refresh_token operation.
     * {@link #REMOVE_TOKENS} Removes all persistent data. Attempted only if revoke tokens succeeds
     * or no flag is set to revoke tokens.
     * {@link #SIGN_OUT_SESSION} Clears browser session if this is a
     * {@link com.okta.oidc.clients.web.WebAuthClient} instance.
     * {@link #ALL} All of the above flags. Same as calling {@link #signOut}
     *
     * @param activity        the activity
     * @param flags           the flag for the operations to perform.
     * @param requestCallback the callback containing the bitwise status. AuthorizationException
     *                        may be null.
     * @see #SUCCESS
     * @see #FAILED_REVOKE_ACCESS_TOKEN
     * @see #FAILED_REVOKE_REFRESH_TOKEN
     * @see #FAILED_CLEAR_DATA
     * @see #FAILED_CLEAR_SESSION
     */
    void signOut(@NonNull Activity activity, int flags,
                 RequestCallback<Integer, AuthorizationException> requestCallback);
}
