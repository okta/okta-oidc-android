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
import com.okta.oidc.clients.BaseAuth;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;

public interface SyncWebAuthClient extends BaseAuth<SyncSessionClient> {
    boolean isInProgress();

    /**
     * Log in using implicit flow.
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     * @return the result
     */
    AuthorizationResult logIn(@NonNull final FragmentActivity activity,
                              @Nullable AuthenticationPayload payload)
            throws InterruptedException;

    /**
     * Sign out from okta. This will clear the browser session
     *
     * @param activity the activity
     * @return the result
     */
    Result signOutFromOkta(@NonNull final FragmentActivity activity)
            throws InterruptedException;

}
