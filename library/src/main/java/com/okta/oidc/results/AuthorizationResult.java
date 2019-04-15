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
package com.okta.oidc.results;

import com.okta.oidc.Tokens;
import com.okta.oidc.util.AuthorizationException;

public class AuthorizationResult extends Result {

    private final Tokens tokens;

    public static AuthorizationResult success(Tokens tokens) {
        return new AuthorizationResult(null, tokens);
    }

    public static AuthorizationResult error(AuthorizationException error) {
        return new AuthorizationResult(error, null);
    }

    AuthorizationResult(AuthorizationException error, Tokens tokens) {
        super(error);
        this.tokens = tokens;
    }

    public Tokens getTokens() {
        return tokens;
    }
}
