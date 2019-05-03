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

package com.okta.oidc;

import androidx.fragment.app.FragmentActivity;

/**
 * The Authorization status returned from a web auth requests.
 * {@link com.okta.oidc.clients.web.WebAuthClient#logIn(FragmentActivity, AuthenticationPayload)}
 * {@link com.okta.oidc.clients.web.WebAuthClient#signOutOfOkta(FragmentActivity)}
 */
public enum AuthorizationStatus {
    /**
     * Authorized authorization status. User is authorized. Received access, refresh, and ID tokens.
     * {@link com.okta.oidc.clients.web.WebAuthClient}
     */
    AUTHORIZED,
    /**
     * In progress authorization status.
     */
    IN_PROGRESS,
    /**
     * Logged out authorization status. Browser session is cleared.
     */
    LOGGED_OUT
}
