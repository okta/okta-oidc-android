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
 * Access tokens include reserved scopes and claims and can optionally include custom scopes and
 * claims.
 * <p>
 * Scopes are requested in the initial authorization request, and the Authorization Server uses
 * the Access Policies to decide whether they can be granted. If any of the requested scopes are
 * rejected by the Access Policies, the request is rejected.
 *
 * @see "Access Token Scopes and Claims
 * <https://developer.okta.com/docs/api/resources/oidc/#access-token-scopes-and-claims>"
 */
public interface Scope {
    /**
     * The openid scope.
     */
    String OPENID = "openid";
    /**
     * The profile scope.
     */
    String PROFILE = "profile";
    /**
     * The email scope.
     */
    String EMAIL = "email";
    /**
     * The phone scope.
     */
    String PHONE = "phone";
    /**
     * The address scope.
     */
    String ADDRESS = "address";
    /**
     * The groups scope.
     */
    String GROUPS = "groups";
    /**
     * The offline_access scope.
     */
    String OFFLINE_ACCESS = "offline_access";
}
