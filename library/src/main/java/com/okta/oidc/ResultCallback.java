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


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A callback that handles a Authorization response returned by Chrome Tabs.
 * Used in onActivityResult to handle the Authorization response.
 *
 * @param <T> the type this request will return on authorized.
 * @param <U> the {@link com.okta.oidc.util.AuthorizationException} type of exception in error
 */
public interface ResultCallback<T, U extends Exception> {

    /**
     * Method called on authorized with a result
     *
     * @param result Result of the authorized request.
     */
    void onSuccess(@NonNull T result);

    /**
     * Method called when login is canceled with a result
     */
    void onCancel();

    /**
     * Method called on error with a the authorized request call
     *
     * @param msg       error message
     * @param exception The {@link com.okta.oidc.util.AuthorizationException} type of exception
     */
    void onError(@Nullable String msg, @Nullable U exception);
}