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
package com.okta.oidc.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.okta.oidc.ResultCallback;

public class MockResultCallback<T, AuthorizationException extends Exception>
        implements ResultCallback<T, AuthorizationException> {

    private AuthorizationException mException;
    private T mResult;
    private String mError;

    @Override
    public void onSuccess(@NonNull T result) {
        mResult = result;
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onError(@Nullable String msg, @Nullable AuthorizationException exception) {
        mError = msg;
        mException = exception;
    }

    public T getResult() {
        return mResult;
    }

    public String getError() {
        return mError;
    }

    public AuthorizationException getException() {
        return mException;
    }
}
