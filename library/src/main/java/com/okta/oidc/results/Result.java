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

import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.util.AuthorizationException;

/**
 * The result of auth and session calls.
 */
public class Result {

    private final AuthorizationException error;
    private boolean isCancel;
    private AuthorizationStatus status;
    private String loginHint;

    /**
     * Instantiates a new Result.
     *
     * @param error    the error
     * @param isCancel the is cancel
     * @param status   the authorization status.
     * @param hint     login hint only set if status is unauthenticated.
     */
    Result(AuthorizationException error, boolean isCancel, AuthorizationStatus status,
            String hint) {
        this.error = error;
        this.isCancel = isCancel;
        this.status = status;
        this.loginHint = hint;
    }

    /**
     * Success result.
     *
     * @return the result
     */
    public static Result success() {
        return new Result(null, false, AuthorizationStatus.AUTHORIZED, null);
    }

    /**
     * Authenticated email verification.
     *
     * @return the result
     */
    public static Result authenticated() {
        return new Result(null, false,
                AuthorizationStatus.EMAIL_VERIFICATION_AUTHENTICATED, null);
    }

    /**
     * Unauthenticated email verification.
     *
     * @param hint the login hint.
     * @return the result
     */
    public static Result unauthenticated(String hint) {
        return new Result(null, false,
                AuthorizationStatus.EMAIL_VERIFICATION_UNAUTHENTICATED, hint);
    }

    /**
     * Cancel result.
     *
     * @return the result
     */
    public static Result cancel() {
        return new Result(new AuthorizationException("Canceled", null), true,
                AuthorizationStatus.CANCELED, null);
    }

    /**
     * Error result.
     *
     * @param error the error
     * @return the result
     */
    public static Result error(AuthorizationException error) {
        return new Result(error, false, AuthorizationStatus.ERROR, null);
    }

    /**
     * Is success boolean.
     *
     * @return the boolean
     */
    public boolean isSuccess() {
        return getError() == null && !isCancel;
    }

    /**
     * Is cancel boolean.
     *
     * @return the boolean
     */
    public boolean isCancel() {
        return isCancel;
    }

    /**
     * Gets error.
     *
     * @return the error
     */
    public AuthorizationException getError() {
        return error;
    }

    /**
     * Get status.
     *
     * @return the AuthorizationStatus
     */
    public AuthorizationStatus getStatus() {
        return status;
    }

    /**
     * Get login hint.
     *
     * @return login hint.
     */
    public String getLoginHint() {
        return loginHint;
    }
}
