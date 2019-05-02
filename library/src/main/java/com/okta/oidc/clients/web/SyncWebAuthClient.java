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
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.clients.BaseAuth;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.results.Result;

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
 *     .withStorage(new SimpleOktaStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .withCallbackExecutor(Executors.newSingleThreadExecutor())
 *     .supportedBrowsers(CHROME_PACKAGE_ID, FIREFOX_PACKAGE_ID)
 *     .create();
 * }
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
    Result signIn(@NonNull FragmentActivity activity,
                              @Nullable AuthenticationPayload payload)
            throws InterruptedException;

    /**
     * Sign out from okta. This will clear the browser session
     *
     * @param activity the activity
     * @return the result
     * @throws InterruptedException the interrupted exception
     */
    Result signOutFromOkta(@NonNull FragmentActivity activity)
            throws InterruptedException;

    void registerCallbackIfInterrupt(FragmentActivity activity,
                                     SyncWebAuthClientImpl.ResultListener resultListener,
                                     ExecutorService executorService);

    void unregisterCallback(FragmentActivity activity);


}
