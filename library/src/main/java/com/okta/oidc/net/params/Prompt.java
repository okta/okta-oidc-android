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
package com.okta.oidc.net.params;

/**
 * Specifies whether the Authorization Server prompts the End-User for re-authentication and consent
 */
@SuppressWarnings("unused")
public interface Prompt {
    /**
     * Do not prompt for authentication or consent. If an Okta session already exists,
     * the user is silently authenticated. Otherwise, an error is returned.
     */
    String NONE = "none";
    /**
     * Always prompt the user for authentication, regardless of whether they have an Okta session.
     */
    String LOGIN = "login";
    /**
     * Depending on the values set for consent_method in the app and and consent on the scope,
     * display the Okta consent dialog, even if the user has already given consent. User consent
     * is available for Custom Authorization Servers (requires the API Access Management feature
     * and the User Consent feature enabled).
     */
    String CONSENT = "consent";
}
