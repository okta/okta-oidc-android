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
import androidx.annotation.Nullable;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.clients.BaseAuth;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.ExecutorService;

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
 * SyncWebAuthClient client = new Okta.SyncWebAuthBuilder()
 *     .withConfig(config)
 *     .withContext(getApplicationContext())
 *     .withStorage(new SharedPreferenceStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .withCallbackExecutor(Executors.newSingleThreadExecutor())
 *     .supportedBrowsers(CHROME_PACKAGE_ID, FIREFOX_PACKAGE_ID)
 *     .create();
 * }*
 * </pre>
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
public interface SyncWebAuthClient extends BaseAuth<SyncSessionClient> {
    /**
     * Checks to see if authentication is in progress.
     *
     * @return the boolean value of true if authentication is in progress otherwise false.
     */
    boolean isInProgress();

    /**
     * Sign in using implicit flow.
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     * @return the result
     * @throws InterruptedException the interrupted exception
     */
    Result signIn(@NonNull Activity activity,
                  @Nullable AuthenticationPayload payload)
            throws InterruptedException;

    /**
     * Sign out from okta. This will clear the browser session
     *
     * @param activity the activity
     * @return the result
     * @throws InterruptedException the interrupted exception
     */
    Result signOutOfOkta(@NonNull Activity activity)
            throws InterruptedException;

    /**
     * Register a callback for sign in and sign out result status. The callback is triggered when
     * {@link #signIn(Activity, AuthenticationPayload) signIn} or
     * {@link #signOutOfOkta(Activity)} signOutOfOkta} is completed but the Activity was interrupted
     * due to it being destroyed during the switch to the web browser.
     * This will set the callback to check if the auth client have a result that can be returned
     * immediately.
     * Example usage:
     * {@code
     * <pre>
     * client.registerCallbackIfInterrupt(activity, (result, type) -> {
     *     switch (type) {
     *         case SIGN_IN:
     *             //process sign-in
     *             break;
     *         case SIGN_OUT:
     *             //process sign-out
     *             break;
     *         default:
     *             break;
     *     }
     * }, Executors.newSingleThreadExecutor());
     * </pre>
     * }*
     *
     * @param resultListener returns the result of sign in or sign out attempts.
     * @param activity       the activity which will receive the results.
     */
    void registerCallbackIfInterrupt(Activity activity,
                                     SyncWebAuthClientImpl.ResultListener resultListener,
                                     ExecutorService executorService);

    /**
     * Unregister callback.
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
     * @param activity the activity
     * @return the bitwise status.
     * @see #SUCCESS
     * @see #FAILED_REVOKE_ACCESS_TOKEN
     * @see #FAILED_REVOKE_REFRESH_TOKEN
     * @see #FAILED_CLEAR_DATA
     * @see #FAILED_CLEAR_SESSION
     */
    int signOut(@NonNull Activity activity);

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
     * @param activity the activity
     * @param flags    the flag for the operations to perform.
     * @return the bitwise status.
     * @see #SUCCESS
     * @see #FAILED_REVOKE_ACCESS_TOKEN
     * @see #FAILED_REVOKE_REFRESH_TOKEN
     * @see #FAILED_CLEAR_DATA
     * @see #FAILED_CLEAR_SESSION
     */
    int signOut(@NonNull Activity activity, int flags);
}
