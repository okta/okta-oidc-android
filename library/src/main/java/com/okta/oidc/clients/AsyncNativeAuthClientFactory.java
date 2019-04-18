package com.okta.oidc.clients;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnectionFactory;

import java.util.concurrent.Executor;

import androidx.annotation.Nullable;

public class AsyncNativeAuthClientFactory extends AuthClientFactory<AsyncNativeAuth> {
    private Executor mCallbackExecutor;

    public AsyncNativeAuthClientFactory(@Nullable Executor executor) {
        this.mCallbackExecutor = executor;
    }

    @Override
    public AsyncNativeAuth createClient(OIDCConfig mOIDCConfig, OktaState mOktaState, HttpConnectionFactory mConnectionFactory) {
        return new AsyncNativeAuthClient(mCallbackExecutor, mOIDCConfig, mOktaState, mConnectionFactory);
    }
}
