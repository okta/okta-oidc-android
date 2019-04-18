package com.okta.oidc.clients.natives;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;

import java.util.concurrent.Executor;

import androidx.annotation.Nullable;

public class AsyncNativeAuthClientFactory extends AuthClientFactory<AsyncNativeAuth> {
    private Executor mCallbackExecutor;

    public AsyncNativeAuthClientFactory(@Nullable Executor executor) {
        this.mCallbackExecutor = executor;
    }

    @Override
    public AsyncNativeAuth createClient(OIDCAccount mOIDCAccount, OktaState mOktaState, HttpConnectionFactory mConnectionFactory) {
        return new AsyncNativeAuthClient(null, mOIDCAccount, mOktaState, mConnectionFactory);
    }
}
