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

import android.app.Activity;

/**
 * The Authorization status returned from a web auth requests.
 * {@link com.okta.oidc.clients.web.WebAuthClient#signIn(Activity, AuthenticationPayload)}
 * {@link com.okta.oidc.clients.web.WebAuthClient#signOutOfOkta(Activity)}
 */
public enum AuthorizationStatus {
    /**
     * Authorized authorization status. User is authorized. Received access, refresh, and ID tokens.
     * {@link com.okta.oidc.clients.web.WebAuthClient}
     */
    AUTHORIZED,
    /**
     * Signed out authorization status. Browser session is cleared.
     */
    SIGNED_OUT,

    /**
     * Operation was canceled.
     */
    CANCELED,

    /**
     * Operation resulted in an exception.
     */
    ERROR,

    /**
     * Email verified and user is authenticated with a valid browser session. Client should handle
     * this status by calling
     * {@link com.okta.oidc.clients.web.WebAuthClient#signIn(Activity, AuthenticationPayload)}
     * again.
     */
    EMAIL_VERIFICATION_AUTHENTICATED,

    /**
     * Email verified but user is not authenticated. User is required to enter credentials.
     * Client should handle this status by calling
     * {@link com.okta.oidc.clients.web.WebAuthClient#signIn(Activity, AuthenticationPayload)}
     * again.
     */
    EMAIL_VERIFICATION_UNAUTHENTICATED
}
