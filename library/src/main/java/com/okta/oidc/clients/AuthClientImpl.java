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

package com.okta.oidc.clients;

import android.content.Context;
import android.os.Process;

import androidx.annotation.AnyThread;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.sessions.SessionClientFactoryImpl;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

class AuthClientImpl implements AuthClient {
    private RequestDispatcher mDispatcher;
    private SyncAuthClient mSyncNativeAuthClient;
    private SessionClient mSessionImpl;
    private volatile Future<?> mFutureTask;

    AuthClientImpl(Executor executor,
                   OIDCConfig oidcConfig,
                   Context context,
                   OktaStorage oktaStorage,
                   EncryptionManager encryptionManager,
                   OktaHttpClient httpClient,
                   boolean requireHardwareBackedKeyStore,
                   boolean cacheMode) {
        mSyncNativeAuthClient = new SyncAuthClientFactory().createClient(oidcConfig, context,
                oktaStorage, encryptionManager, httpClient,
                requireHardwareBackedKeyStore, cacheMode);

        mSessionImpl = new SessionClientFactoryImpl(executor)
                .createClient(mSyncNativeAuthClient.getSessionClient());
        mDispatcher = new RequestDispatcher(executor);
    }

    @Override
    @AnyThread
    public void signIn(String sessionToken, AuthenticationPayload payload,
                       final RequestCallback<Result, AuthorizationException> cb) {
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Result result = mSyncNativeAuthClient.signIn(sessionToken, payload);
            if (result.isSuccess()) {
                mDispatcher.submitResults(() -> {
                    if (cb != null) {
                        cb.onSuccess(result);
                    }

                });
            } else {
                mDispatcher.submitResults(() -> {
                    if (cb != null) {
                        cb.onError(result.getError().error, result.getError());
                    }
                });
            }
        });
    }

    @Override
    public void cancel() {
        mDispatcher.runTask(() -> {
            mSyncNativeAuthClient.cancel();
            cancelFuture();
        });
    }

    @Override
    public void migrateTo(EncryptionManager manager) throws AuthorizationException {
        getSessionClient().migrateTo(manager);
    }

    @Override
    public SessionClient getSessionClient() {
        return mSessionImpl;
    }

    @Override
    public void signOut(ResultCallback<Integer, AuthorizationException> callback) {
        signOut(ALL, callback);
    }

    @Override
    public void signOut(int flags, ResultCallback<Integer, AuthorizationException> callback) {
        mFutureTask = mDispatcher.submit(() -> {
            final int status = mSyncNativeAuthClient.signOut(flags);
            mDispatcher.submitResults(() -> {
                if (callback != null) {
                    callback.onSuccess(status);
                }
            });
        });
    }

    private void cancelFuture() {
        if (mFutureTask != null && (!mFutureTask.isDone() || !mFutureTask.isCancelled())) {
            mFutureTask.cancel(true);
        }
    }
}
