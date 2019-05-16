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

import androidx.annotation.NonNull;

import com.okta.oidc.RequestCallback;

import java.util.concurrent.CountDownLatch;

public class MockRequestCallback<T, U extends Exception> implements RequestCallback<T, U> {
    private U mException;
    private T mResult;
    private String mError;
    private CountDownLatch mLatch;

    public MockRequestCallback(CountDownLatch latch) {
        mLatch = latch;
    }

    @Override
    public void onSuccess(@NonNull T result) {
        mResult = result;
        mLatch.countDown();
    }

    @Override
    public void onError(String error, U exception) {
        mError = error;
        mException = exception;
        mLatch.countDown();
    }

    public U getException() {
        return mException;
    }

    public String getError() {
        return mError;
    }

    public T getResult() {
        return mResult;
    }
}
