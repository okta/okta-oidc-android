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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class SessionClientImpl implements SessionClient {
    private final SyncSessionClient mSyncSessionClient;
    private final RequestDispatcher mDispatcher;
    private volatile Future<?> mFutureTask;
    private final List<RequestCallback<Tokens, AuthorizationException>>
            refreshTokenRequestCallbacks;
    private final Executor serialExecutor = Executors.newSingleThreadExecutor();

    SessionClientImpl(Executor callbackExecutor, SyncSessionClient syncSessionClient) {
        mSyncSessionClient = syncSessionClient;
        mDispatcher = new RequestDispatcher(callbackExecutor);
        refreshTokenRequestCallbacks = new ArrayList<>();
    }

    public void getUserProfile(RequestCallback<UserInfo, AuthorizationException> cb) {
        CallbackWrapper<UserInfo, AuthorizationException> wrapper = new CallbackWrapper<>(cb);
        executeSerial(wrapper, () -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                UserInfo userInfo = mSyncSessionClient.getUserProfile();
                mDispatcher.submitResults(() -> wrapper.onSuccess(userInfo));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> wrapper.onError(ae.error, ae));
            } catch (Exception ex) {
                mDispatcher.submitResults(() -> wrapper.onError(ex.getMessage(),
                        new AuthorizationException(ex.getMessage(), ex)));
            }
        });
    }

    public void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectInfo, AuthorizationException> cb) {
        CallbackWrapper<IntrospectInfo, AuthorizationException> wrapper = new CallbackWrapper<>(cb);
        executeSerial(wrapper, () -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                IntrospectInfo introspectInfo = mSyncSessionClient
                        .introspectToken(token, tokenType);
                mDispatcher.submitResults(() -> wrapper.onSuccess(introspectInfo));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> wrapper.onError(ae.error, ae));
            } catch (Exception ex) {
                mDispatcher.submitResults(() -> wrapper.onError(ex.getMessage(),
                        new AuthorizationException(ex.getMessage(), ex)));
            }
        });
    }

    public void revokeToken(String token,
                            final RequestCallback<Boolean, AuthorizationException> cb) {
        CallbackWrapper<Boolean, AuthorizationException> wrapper = new CallbackWrapper<>(cb);
        executeSerial(wrapper, () -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                Boolean isRevoke = mSyncSessionClient.revokeToken(token);
                mDispatcher.submitResults(() -> wrapper.onSuccess(isRevoke));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> wrapper.onError(ae.error, ae));
            } catch (Exception ex) {
                mDispatcher.submitResults(() -> wrapper.onError(ex.getMessage(),
                        new AuthorizationException(ex.getMessage(), ex)));
            }
        });
    }

    public void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) {
        //Wrap the callback from the app because we want to be consistent in
        //returning a Tokens object instead of a TokenResponse.
        boolean isEmpty;
        if (Thread.holdsLock(refreshTokenRequestCallbacks)) {
            throw new RuntimeException("refreshToken can't be called from callback.");
        }
        CallbackWrapper<Tokens, AuthorizationException> wrapper = new CallbackWrapper<>(cb);
        synchronized (refreshTokenRequestCallbacks) {
            isEmpty = refreshTokenRequestCallbacks.isEmpty();
            refreshTokenRequestCallbacks.add(wrapper);
        }
        if (isEmpty) {
            executeSerial(wrapper, () -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    Tokens result = mSyncSessionClient.refreshToken();
                    mDispatcher.submitResults(() -> {
                        synchronized (refreshTokenRequestCallbacks) {
                            for (RequestCallback<Tokens, AuthorizationException> callback
                                    : refreshTokenRequestCallbacks) {
                                callback.onSuccess(result);
                            }
                            refreshTokenRequestCallbacks.clear();
                        }
                    });
                } catch (AuthorizationException ae) {
                    mDispatcher.submitResults(() -> {
                        synchronized (refreshTokenRequestCallbacks) {
                            for (RequestCallback<Tokens, AuthorizationException> callback
                                    : refreshTokenRequestCallbacks) {
                                callback.onError(ae.error, ae);
                            }
                            refreshTokenRequestCallbacks.clear();
                        }
                    });
                } catch (Exception ex) {
                    mDispatcher.submitResults(() -> {
                        synchronized (refreshTokenRequestCallbacks) {
                            for (RequestCallback<Tokens, AuthorizationException> callback
                                    : refreshTokenRequestCallbacks) {
                                callback.onError(ex.getMessage(),
                                        new AuthorizationException(ex.getMessage(), ex));
                            }
                            refreshTokenRequestCallbacks.clear();
                        }
                    });
                }
            });
        }
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
        CallbackWrapper<JSONObject, AuthorizationException> wrapper = new CallbackWrapper<>(cb);
        executeSerial(wrapper, () -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                JSONObject result = mSyncSessionClient
                        .authorizedRequest(uri, properties, postParameters, method);
                mDispatcher.submitResults(() -> wrapper.onSuccess(result));
            } catch (AuthorizationException ae) {
                mDispatcher.submitResults(() -> wrapper.onError(ae.error, ae));
            } catch (Exception ex) {
                mDispatcher.submitResults(() -> wrapper.onError(ex.getMessage(),
                        new AuthorizationException(ex.getMessage(), ex)));
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

    private void executeSerial(CallbackWrapper<?, ?> callback, Runnable runnable) {
        serialExecutor.execute(() -> {
            cancelFuture();
            mFutureTask = mDispatcher.submit(runnable);
            callback.waitForCallback();
        });
    }

    private static class CallbackWrapper<T, U extends Exception> implements RequestCallback<T, U> {
        private static final int MAX_WAIT_MINUTES = 5;
        private final RequestCallback<T, U> delegate;
        private final CountDownLatch latch = new CountDownLatch(1);

        CallbackWrapper(RequestCallback<T, U> delegate) {
            this.delegate = delegate;
        }

        @Override public void onSuccess(@NonNull T result) {
            delegate.onSuccess(result);
            latch.countDown();
        }

        @Override public void onError(String error, U exception) {
            delegate.onError(error, exception);
            latch.countDown();
        }

        boolean waitForCallback() {
            try {
                return latch.await(MAX_WAIT_MINUTES, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
