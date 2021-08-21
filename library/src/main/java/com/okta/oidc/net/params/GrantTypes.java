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
 * Determines the mechanism Okta uses to authorize the creation of the tokens.
 */
public final class GrantTypes {
    /**
     * The grant type for authorization code.
     */
    public static final String AUTHORIZATION_CODE = "authorization_code";
    /**
     * The grant type for refreshing a access token.
     */
    public static final String REFRESH_TOKEN = "refresh_token";
    /**
     * The grant type for exchanging a device secret.
     */
    public static final String  DEVICE_SECRET = "urn:ietf:params:oauth:grant-type:token-exchange";

    private GrantTypes() {
        throw new AssertionError();
    }
}
