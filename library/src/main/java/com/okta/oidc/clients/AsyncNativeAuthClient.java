package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.clients.sessions.AsyncSessionClient;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;

class AsyncNativeAuthClient implements AsyncNativeAuth {
    private RequestDispatcher mDispatcher;
    private SyncNativeAuthClient mSyncNativeAuthClient;
    private AsyncSessionClient sessionClient;

    AsyncNativeAuthClient(Executor executor, OIDCConfig oidcConfig, OktaState oktaState, HttpConnectionFactory httpConnectionFactory) {
        this.mSyncNativeAuthClient = new SyncNativeAuthClient(oidcConfig, oktaState, httpConnectionFactory);
        this.sessionClient = new AsyncSessionClient(executor, oidcConfig, oktaState, httpConnectionFactory);
        mDispatcher = new RequestDispatcher(executor);
    }

    @Override
    @AnyThread
    public void logIn(String sessionToken, AuthenticationPayload payload, RequestCallback<AuthorizationResult, AuthorizationException> cb) {
        mDispatcher.execute(() -> {
           AuthorizationResult result = mSyncNativeAuthClient.logIn(sessionToken, payload);
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
    public AsyncSession getSessionClient() {
        return this.sessionClient;
    }
}
