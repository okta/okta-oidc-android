package com.okta.oidc;

import android.content.Context;

import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.sessions.SessionClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;

import androidx.annotation.NonNull;


//public Okta.Builder withTabColor(@ColorInt int customTabColor) {
//        mCustomTabColor = customTabColor;
//        return this;
//        }
//
//public Okta.Builder withCallbackExecutor(Executor executor) {
//        mCallbackExecutor = executor;
//        return this;
//        }


//
//public Okta.Builder supportedBrowsers(String... browsers) {
//        mSupportedBrowsers = browsers;
//        return this;
//        }

public class Okta<A,S> {

    private HttpConnectionFactory mConnectionFactory;
    private OIDCAccount mOIDCAccount;
    private OktaState mOktaState;

    private AuthClientFactory<A> authClientFactory;
    private SessionClientFactory<S> sessionClientFactory;

    public Okta(Builder<A,S>  builder) {
        this.mConnectionFactory = builder.mConnectionFactory;
        this.mOIDCAccount = builder.mOIDCAccount;
        this.mOktaState = new OktaState(new OktaRepository(builder.mStorage, builder.context));
        this.authClientFactory = builder.authClientFactory;
        this.sessionClientFactory = builder.sessionClientFactory;
    }

    public A getAuthorizationClient() {
        return authClientFactory.createClient(mOIDCAccount, mOktaState, mConnectionFactory);
    }

    public S getSessionClient() {
        return sessionClientFactory.createSession(mOIDCAccount, mOktaState, mConnectionFactory);
    }

    public static class Builder<A,S> {
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private OktaStorage mStorage;
        private AuthClientFactory<A> authClientFactory;
        private SessionClientFactory<S> sessionClientFactory;
        private Context context;

        public Builder() {
        }

        public Okta<A,S> create() {
            return new Okta<>(this);
        }

        public Okta.Builder<A,S> withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        public Okta.Builder<A,S> withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

        public Okta.Builder<A,S> withContext(Context context) {
            this.context = context;
            return this;
        }

        public Okta.Builder<A,S> withStorage(OktaStorage storage) {
            this.mStorage = storage;
            return this;
        }

        public Okta.Builder<A,S> withAuthenticationClientFactory(AuthClientFactory<A> authClientFactory) {
            this.authClientFactory = authClientFactory;
            return this;
        }

        public Okta.Builder<A,S> withSessionClientFactory(SessionClientFactory<S> sessionClientFactory) {
            this.sessionClientFactory = sessionClientFactory;
            return this;
        }
    }
}
