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

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.BaseAuth;
import com.okta.oidc.clients.sessions.SessionClient;
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
 *     .withStorage(new SimpleOktaStorage(this))
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
     * Log in using implicit flow.
     *
     * <p>The result will be returned in the
     * {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     */
    void logIn(@NonNull FragmentActivity activity, AuthenticationPayload payload);

    /**
     * Sign out from okta. This will clear the browser session
     *
     * <p>The result will be returned in the
     * {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param activity the activity
     */
    void signOutFromOkta(@NonNull FragmentActivity activity);

    /**
     * Register a callback for login and logout result status. The callback is triggered when
     * {@link #logIn(FragmentActivity, AuthenticationPayload) logIn} or
     * {@link #signOutFromOkta(FragmentActivity)} signOutFromOkta} is completed.
     * Example usage:
     * {@code
     * <pre>
     * client.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
     *      @Override
     *      public void onSuccess(@NonNull AuthorizationStatus status) {
     *          if (status == AuthorizationStatus.AUTHORIZED) {
     *              //login success. client can be used to perform protected resource requests.
     *          } else if (status == AuthorizationStatus.LOGGED_OUT) {
     *              //logout success. browser session is cleared.
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
     * @param resultCallback returns the result of login or logout attempts.
     * @param activity       the activity which will receive the results.
     */
    void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException>
                                  resultCallback, FragmentActivity activity);

    /**
     * Unregister the callback.
     */
    void unregisterCallback();
}
