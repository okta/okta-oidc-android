package com.okta.oidc;

import android.content.Context;

import androidx.annotation.NonNull;

import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;

public abstract class OktaBuilder<A,T extends OktaBuilder<A,T>> {
    HttpConnectionFactory mConnectionFactory;
    OIDCConfig mOIDCConfig;
    OktaStorage mStorage;
    Context context;
    AuthClientFactory<A> authClientFactory;

    abstract T toThis();

    abstract A create();

    public T withConfig(@NonNull OIDCConfig config) {
        mOIDCConfig = config;
        return toThis();
    }

    public T withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
        mConnectionFactory = connectionFactory;
        return toThis();
    }

    public T withContext(Context context) {
        this.context = context;
        return toThis();
    }

    public T withStorage(OktaStorage storage) {
        this.mStorage = storage;
        return toThis();
    }

    T withAuthenticationClientFactory(AuthClientFactory<A> authClientFactory) {
        this.authClientFactory = authClientFactory;
        return toThis();
    }

    protected A createAuthClient() {
        return this.authClientFactory.createClient(mOIDCConfig, new OktaState(new OktaRepository(mStorage, context)), mConnectionFactory);
    }
}
