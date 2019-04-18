package com.okta.oidc.sessions;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

import java.util.concurrent.Executor;

import androidx.annotation.Nullable;

public class AsyncSessionClientFactory extends SessionClientFactory<AsyncSession> {
    private Executor callbackExecutor;

    public AsyncSessionClientFactory(@Nullable Executor callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public AsyncSession createSession(OIDCAccount oidcAccount, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        return new AsyncSessionClient(callbackExecutor, oidcAccount, oktaState, connectionFactory);
    }
}
