package com.okta.oidc.clients;

import android.app.Activity;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.clients.sessions.AsyncSessionClient;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;
import com.okta.oidc.util.AuthorizationException;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

class AsyncWebAuthClient implements AsyncWebAuth {
    private WeakReference<FragmentActivity> mActivity;
    private RequestDispatcher mDispatcher;
    private ResultCallback<AuthorizationStatus, AuthorizationException> mResultCb;
    private SyncWebAuthClient mSyncAuthClient;
    private AsyncSessionClient sessionClient;


    AsyncWebAuthClient(Executor executor, OIDCConfig oidcConfig, OktaState oktaState, HttpConnectionFactory httpConnectionFactory, String[] supportedBrowsers, int customTabColor) {
        this.mSyncAuthClient = new SyncWebAuthClient(oidcConfig, oktaState, httpConnectionFactory, supportedBrowsers, customTabColor);
        this.sessionClient = new AsyncSessionClient(executor, oidcConfig, oktaState, httpConnectionFactory);
        mDispatcher = new RequestDispatcher(executor);
    }

    private void registerActivityLifeCycle(@NonNull final FragmentActivity activity) {
        mActivity = new WeakReference<>(activity);
        mActivity.get().getApplication().registerActivityLifecycleCallbacks(new EmptyActivityLifeCycle() {
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
    public void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException> resultCallback, FragmentActivity activity) {
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
            }
        }, mDispatcher);

    }

    @Override
    public void unregisterCallback() {
        mResultCb = null;
        if(mActivity.get()!= null) {
            mSyncAuthClient.unregisterCallback(mActivity.get());
        }
    }

    @Override
    public boolean isInProgress() {
        return mSyncAuthClient.isInProgress();
    }

    @Override
    @AnyThread
    public void logIn(@NonNull final FragmentActivity activity, AuthenticationPayload payload) {
        if (activity != null) {
            registerActivityLifeCycle(activity);
        }
        mDispatcher.execute(() -> {
            try {
                AuthorizationResult result = mSyncAuthClient.logIn(activity, payload);

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
    public void signOutFromOkta(@NonNull final FragmentActivity activity) {
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
    public AsyncSession getSessionClient() {
        return this.sessionClient;
    }
}
