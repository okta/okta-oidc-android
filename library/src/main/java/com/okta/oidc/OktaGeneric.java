package com.okta.oidc;

import com.okta.oidc.factory.client.AuthClientFactory;
import com.okta.oidc.factory.session.SessionClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;
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

//public Okta.Builder withContext(Context context) {
//        this.mContext = context;
//        return this;
//        }
//
//public Okta.Builder supportedBrowsers(String... browsers) {
//        mSupportedBrowsers = browsers;
//        return this;
//        }

public class OktaGeneric<B,N,S> {

    private HttpConnectionFactory mConnectionFactory;
    private OIDCAccount mOIDCAccount;
    private OktaStorage mStorage;

    private B browserAuthenticationClient;
    private N nativeAuthenticationClient;
    private S sessionClient;

    public OktaGeneric(Builder<B,N,S>  builder) {
        this.mConnectionFactory = builder.mConnectionFactory;
        this.mOIDCAccount = builder.mOIDCAccount;
        this.mStorage = builder.mStorage;
        this.browserAuthenticationClient = builder.authClientFactory.createBrowser();
        this.nativeAuthenticationClient = builder.authClientFactory.createNative();
        this.sessionClient = builder.sessionClientFactory.createSession();
    }

    public B getBrowserAuthorizationClient() {
        return browserAuthenticationClient;
    }

    public N getNativeAuthorizationClient() {
        return nativeAuthenticationClient;
    }

    public S getSessionClient() {
        return sessionClient;
    }

    public static class Builder<B,N,S> {
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private OktaStorage mStorage;
        private AuthClientFactory<B,N> authClientFactory;
        private SessionClientFactory<S> sessionClientFactory;

        public Builder() {
        }

        public OktaGeneric<B,N,S> create() {
            return new OktaGeneric<>(this);
        }

        public OktaGeneric.Builder<B,N,S> withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        public OktaGeneric.Builder<B,N,S> withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

        public OktaGeneric.Builder<B,N,S> withStorage(OktaStorage storage) {
            this.mStorage = storage;
            return this;
        }

        public OktaGeneric.Builder<B,N,S> withAuthenticationClientFactory(AuthClientFactory<B,N> authClientFactory) {
            this.authClientFactory = authClientFactory;
            return this;
        }

        public OktaGeneric.Builder<B,N,S> withSessionClientFactory(SessionClientFactory<S> sessionClientFactory) {
            this.sessionClientFactory = sessionClientFactory;
            return this;
        }
    }
}
