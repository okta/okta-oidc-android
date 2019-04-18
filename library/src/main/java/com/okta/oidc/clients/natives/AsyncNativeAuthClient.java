package com.okta.oidc.clients.natives;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;

class AsyncNativeAuthClient implements AsyncNativeAuth {
    private RequestDispatcher mDispatcher;
    private SyncNativeAuthClient mSyncNativeAuthClient;

    AsyncNativeAuthClient(Executor executor, OIDCAccount oidcAccount, OktaState oktaState, HttpConnectionFactory httpConnectionFactory) {
        this.mSyncNativeAuthClient = new SyncNativeAuthClient(oidcAccount, oktaState, httpConnectionFactory);
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
}
