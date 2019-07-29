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

package com.okta.oidc.clients.sessions;

import android.net.Uri;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

class SessionClientImpl implements SessionClient {
    private SyncSessionClient mSyncSessionClient;
    private RequestDispatcher mDispatcher;
    private volatile Future<?> mFutureTask;

    SessionClientImpl(Executor callbackExecutor, SyncSessionClient syncSessionClient) {
        mSyncSessionClient = syncSessionClient;
        mDispatcher = new RequestDispatcher(callbackExecutor);
    }

    public void getUserProfile(final RequestCallback<UserInfo, AuthorizationException> cb) {
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                UserInfo userInfo = mSyncSessionClient.getUserProfile();
                mDispatcher.submitResults(() -> cb.onSuccess(userInfo));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> cb.onError(ae.error, ae));
            }
        });
    }

    public void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectInfo, AuthorizationException> cb) {
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                IntrospectInfo introspectInfo = mSyncSessionClient
                        .introspectToken(token, tokenType);
                mDispatcher.submitResults(() -> cb.onSuccess(introspectInfo));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> cb.onError(ae.error, ae));
            }
        });
    }

    public void revokeToken(String token,
                            final RequestCallback<Boolean, AuthorizationException> cb) {
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                Boolean isRevoke = mSyncSessionClient.revokeToken(token);
                mDispatcher.submitResults(() -> cb.onSuccess(isRevoke));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> cb.onError(ae.error, ae));
            }
        });
    }

    public void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) {
        //Wrap the callback from the app because we want to be consistent in
        //returning a Tokens object instead of a TokenResponse.
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                Tokens result = mSyncSessionClient.refreshToken();
                mDispatcher.submitResults(() -> cb.onSuccess(result));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> cb.onError(ae.error, ae));
            }
        });
    }

    @Override
    public Tokens getTokens() throws AuthorizationException {
        return mSyncSessionClient.getTokens();
    }

    @Override
    public void authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                  @Nullable Map<String, String> postParameters,
                                  @NonNull ConnectionParameters.RequestMethod method,
                                  final RequestCallback<JSONObject, AuthorizationException> cb) {
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                JSONObject result = mSyncSessionClient
                        .authorizedRequest(uri, properties, postParameters, method);
                mDispatcher.submitResults(() -> cb.onSuccess(result));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> cb.onError(ae.error, ae));
            }
        });
    }

    @Override
    public void authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                  @Nullable Map<String, String> postParameters,
                                  @Nullable byte[] requestBody,
                                  @NonNull ConnectionParameters.RequestMethod method,
                                  final RequestCallback<ByteBuffer, AuthorizationException> cb) {
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                ByteBuffer byteBuffer = mSyncSessionClient
                        .authorizedRequest(uri, properties, postParameters, requestBody, method);
                mDispatcher.submitResults(() -> cb.onSuccess(byteBuffer));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> cb.onError(ae.error, ae));
            }
        });
    }

    public boolean isAuthenticated() {
        return mSyncSessionClient.isAuthenticated();
    }

    public void clear() {
        mSyncSessionClient.clear();
    }

    @Override
    public void cancel() {
        mDispatcher.runTask(() -> {
            mSyncSessionClient.cancel();
            cancelFuture();
        });
    }

    @Override
    public void migrateTo(EncryptionManager manager) throws AuthorizationException {
        mSyncSessionClient.migrateTo(manager);
    }

    private void cancelFuture() {
        if (mFutureTask != null && (!mFutureTask.isDone() || !mFutureTask.isCancelled())) {
            mFutureTask.cancel(true);
        }
    }
}
