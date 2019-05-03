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

import androidx.annotation.Nullable;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.util.AuthorizationException;

/**
 * The Authorization result returned from a auth requests.
 * {@link com.okta.oidc.clients.AuthClient#signIn(String, AuthenticationPayload, RequestCallback)}
 */
public class AuthorizationResult extends Result {

    private final Tokens tokens;

    /**
     * Creates a successful authorization result with tokens.
     * {@link Tokens}
     *
     * @param tokens the tokens
     * @return the authorization result
     */
    public static AuthorizationResult success(Tokens tokens) {
        return new AuthorizationResult(null, tokens, false);
    }

    /**
     * Creates a authorization result with a AuthorizationException.
     * {@link AuthorizationException}
     *
     * @param error the error
     * @return the authorization result with exception
     */
    public static AuthorizationResult error(AuthorizationException error) {
        return new AuthorizationResult(error, null, false);
    }

    /**
     * Creates a authorization result cancel flag set to true.
     *
     * @return the authorization result with cancel perameter
     */
    public static AuthorizationResult cancel() {
        return new AuthorizationResult(null, null, true);
    }

    private AuthorizationResult(AuthorizationException error, Tokens tokens, boolean isCancel) {
        super(error, isCancel);
        this.tokens = tokens;
    }

    /**
     * Gets tokens of a successful authorization.
     *
     * @return the tokens
     */
    @Nullable
    public Tokens getTokens() {
        return tokens;
    }
}
