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

package com.okta.oidc.clients.web;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.CustomTabOptions;
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

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

class WebAuthClientImpl implements WebAuthClient {
    private WeakReference<Activity> mActivity;
    private RequestDispatcher mDispatcher;
    private ResultCallback<AuthorizationStatus, AuthorizationException> mResultCb;
    private SyncWebAuthClient mSyncAuthClient;
    private SessionClient mSessionImpl;
    private volatile Future<?> mFutureTask;
    private String mLoginHint;

    WebAuthClientImpl(Executor executor, OIDCConfig oidcConfig,
                      Context context,
                      OktaStorage oktaStorage,
                      EncryptionManager encryptionManager,
                      OktaHttpClient httpClient,
                      boolean requireHardwareBackedKeyStore,
                      boolean cacheMode,
                      boolean isResultFragmentEnabled,
                      CustomTabOptions customTabOptions,
                      String... supportedBrowsers) {
        mSyncAuthClient = new SyncWebAuthClientFactory(customTabOptions, isResultFragmentEnabled, supportedBrowsers)
                .createClient(oidcConfig, context, oktaStorage, encryptionManager,
                        httpClient, requireHardwareBackedKeyStore, cacheMode);
        mSessionImpl = new SessionClientFactoryImpl(executor)
                .createClient(mSyncAuthClient.getSessionClient());
        mDispatcher = new RequestDispatcher(executor);
    }

    private void registerActivityLifeCycle(@NonNull final Activity activity) {
        mActivity = new WeakReference<>(activity);
        mActivity.get().getApplication()
                .registerActivityLifecycleCallbacks(new EmptyActivityLifeCycle() {
                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (mActivity != null && mActivity.get() == activity) {
                            stop();
                            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                        }
                    }
                });
    }

    private void stop() {
        unregisterCallback();
        mDispatcher.stopAllTasks();
    }

    @Override
    public void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException>
                                         resultCallback, Activity activity) {
        mResultCb = resultCallback;
        registerActivityLifeCycle(activity);
        mSyncAuthClient.registerCallbackIfInterrupt(activity, (result, type) -> {
            switch (type) {
                case SIGN_IN:
                    processSignInResult(result);
                    break;
                case SIGN_OUT:
                    processSignOutResult(result);
                    break;
                default:
                    break;
            }
        }, mDispatcher);

    }

    @Override
    public void unregisterCallback() {
        mResultCb = null;
        if (mActivity.get() != null) {
            mSyncAuthClient.unregisterCallback();
        }
    }

    @Override
    public void cancel() {
        mDispatcher.runTask(() -> {
            mSyncAuthClient.cancel();
            cancelFuture();
        });
    }

    @Override
    public boolean isInProgress() {
        return mSyncAuthClient.isInProgress();
    }

    @Override
    @AnyThread
    public void signIn(@NonNull final Activity activity, AuthenticationPayload payload) {
        registerActivityLifeCycle(activity);
        cancelFuture();
        //add the login hint if it exists.
        if (payload == null && mLoginHint != null) {
            payload = new AuthenticationPayload.Builder().setLoginHint(mLoginHint).build();
        } else if (mLoginHint != null) {
            payload = new AuthenticationPayload.Builder()
                    .copyPayload(payload)
                    .setLoginHint(mLoginHint)
                    .build();
        }
        final AuthenticationPayload finalPayload = payload;
        mFutureTask = mDispatcher.submit(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            try {
                Result result = mSyncAuthClient.signIn(activity, finalPayload);
                processSignInResult(result);
            } catch (InterruptedException e) {
                mDispatcher.submitResults(() -> {
                    if (mResultCb != null) {
                        mResultCb.onCancel();
                    }
                });
            }
        });
    }

    private void processSignInResult(Result result) {
        if (result.getStatus() == AuthorizationStatus.EMAIL_VERIFICATION_UNAUTHENTICATED) {
            mLoginHint = result.getLoginHint();
        } else {
            mLoginHint = null;
        }

        if (mResultCb == null) {
            return; //do nothing since no callback registered.
        }
        if (result.isSuccess()) {
            mDispatcher.submitResults(() -> mResultCb.onSuccess(result.getStatus()));
        } else if (result.isCancel()) {
            mDispatcher.submitResults(() -> mResultCb.onCancel());
        } else {
            mDispatcher.submitResults(() -> mResultCb.onError("Authorization error",
                    result.getError()));
        }
    }

    @Override
    @AnyThread
    public void signOutOfOkta(@NonNull final Activity activity) {
        registerActivityLifeCycle(activity);
        cancelFuture();
        mFutureTask = mDispatcher.submit(() -> {
            try {
                Result result = mSyncAuthClient.signOutOfOkta(activity);
                processSignOutResult(result);
            } catch (InterruptedException e) {
                mDispatcher.submitResults(() -> {
                    if (mResultCb != null) {
                        mResultCb.onCancel();
                    }
                });
            }
        });
    }

    private void processSignOutResult(Result result) {
        if (mResultCb == null) {
            return; //do nothing since no callback registered.
        }
        if (result.isSuccess()) {
            mDispatcher.submitResults(() -> mResultCb.onSuccess(AuthorizationStatus.SIGNED_OUT));
        } else if (result.isCancel()) {
            mDispatcher.submitResults(() -> mResultCb.onCancel());
        } else {
            mDispatcher.submitResults(() -> mResultCb.onError("Log out error",
                    result.getError()));
        }
    }

    @Override
    public void migrateTo(EncryptionManager manager) throws AuthorizationException {
        getSessionClient().migrateTo(manager);
    }

    @Override
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        mSyncAuthClient.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public SessionClient getSessionClient() {
        return mSessionImpl;
    }

    @Override
    public void signOut(@NonNull final Activity activity,
                        RequestCallback<Integer, AuthorizationException> callback) {
        signOut(activity, ALL, callback);
    }

    @Override
    public void signOut(@NonNull final Activity activity, int flags,
                        RequestCallback<Integer, AuthorizationException> callback) {
        mFutureTask = mDispatcher.submit(() -> {
            final int status = mSyncAuthClient.signOut(activity, flags);
            mDispatcher.submitResults(() -> {
                if (callback != null) {
                    callback.onSuccess(status);
                }
            });
        });
    }

    private void cancelFuture() {
        if (mFutureTask != null && (!mFutureTask.isDone() && !mFutureTask.isCancelled())) {
            mFutureTask.cancel(true);
        }
    }
}
