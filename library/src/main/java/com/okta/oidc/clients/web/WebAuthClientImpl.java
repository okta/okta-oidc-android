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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.sessions.SessionClientImpl;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;
import com.okta.oidc.util.AuthorizationException;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

class WebAuthClientImpl implements WebAuthClient {
    private WeakReference<FragmentActivity> mActivity;
    private RequestDispatcher mDispatcher;
    private ResultCallback<AuthorizationStatus, AuthorizationException> mResultCb;
    private SyncWebAuthClientImpl mSyncAuthClient;
    private SessionClientImpl mSessionImpl;

    WebAuthClientImpl(Executor executor, OIDCConfig oidcConfig, OktaState oktaState,
                      HttpConnectionFactory httpConnectionFactory,
                      int customTabColor, String... supportedBrowsers) {
        mSyncAuthClient = new SyncWebAuthClientImpl(oidcConfig, oktaState,
                httpConnectionFactory, customTabColor, supportedBrowsers);
        mSessionImpl = new SessionClientImpl(executor, oidcConfig, oktaState,
                httpConnectionFactory);
        mDispatcher = new RequestDispatcher(executor);
    }

    private void registerActivityLifeCycle(@NonNull final FragmentActivity activity) {
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
        mResultCb = null;
        mDispatcher.shutdown();
    }

    @Override
    public void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException>
                                         resultCallback, FragmentActivity activity) {
        mResultCb = resultCallback;
        registerActivityLifeCycle(activity);
        mSyncAuthClient.registerCallbackIfInterrupt(activity, (result, type) -> {
            switch (type) {
                case SIGN_IN:
                    processLogInResult((AuthorizationResult) result);
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
            mSyncAuthClient.unregisterCallback(mActivity.get());
        }
    }

    @Override
    public boolean isInProgress() {
        return mSyncAuthClient.isInProgress();
    }

    @Override
    @AnyThread
    public void signIn(@NonNull final FragmentActivity activity, AuthenticationPayload payload) {
        registerActivityLifeCycle(activity);
        mDispatcher.execute(() -> {
            try {
                AuthorizationResult result = mSyncAuthClient.signIn(activity, payload);

                processLogInResult(result);
            } catch (InterruptedException e) {
                mDispatcher.submitResults(() -> {
                    if (mResultCb != null) {
                        mResultCb.onCancel();
                    }
                });
            }
        });
    }

    private void processLogInResult(AuthorizationResult result) {
        if (result.isSuccess()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onSuccess(
                            AuthorizationStatus.AUTHORIZED);
                }
            });
        } else if (result.isCancel()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onCancel();
                }
            });
        } else {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onError("Authorization error",
                            result.getError());
                }
            });
        }
    }

    @Override
    @AnyThread
    public void signOutOfOkta(@NonNull final FragmentActivity activity) {
        registerActivityLifeCycle(activity);
        mDispatcher.execute(() -> {
            try {
                Result result = mSyncAuthClient.signOutFromOkta(activity);
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
        if (result.isSuccess()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onSuccess(
                            AuthorizationStatus.LOGGED_OUT);
                }
            });
        } else if (result.isCancel()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onCancel();
                }
            });
        } else {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onError("Log out error",
                            result.getError());
                }
            });
        }
    }

    @Override
    public SessionClient getSessionClient() {
        return mSessionImpl;
    }
}
