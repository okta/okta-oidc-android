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
 * Indicates the type of token being passed in a request.
 */
public final class TokenTypeHint {
    /**
     * The type of token is a access_token.
     */
    public static final String ACCESS_TOKEN = "access_token";
    /**
     * The type of token is a id_token.
     */
    public static final String ID_TOKEN = "id_token";
    /**
     * The type of token is a refresh_token.
     */
    public static final String REFRESH_TOKEN = "refresh_token";
    /**
     * The type of token is a device_secret.
     */
    public static final String DEVICE_SECRET = "device_secret";
    /**
     * The type of actor token.
     */
    public static final String ACTOR_TOKEN_TYPE = "urn:x-oath:params:oauth:token-type:device-secret";
    /**
     * The type of subject token.
     */
    public static final String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:id_token";

    private TokenTypeHint() {
        throw new AssertionError();
    }
}
