package com.okta.oidc;

import com.okta.oidc.factory.client.AuthClientFactory;
import com.okta.oidc.factory.client.IBrowserAuthClient;
import com.okta.oidc.factory.client.INativeAuthClient;
import com.okta.oidc.factory.session.ISessionClient;
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

public class Okta {

    private HttpConnectionFactory mConnectionFactory;
    private OIDCAccount mOIDCAccount;
    private OktaStorage mStorage;
    private AuthClientFactory authClientFactory;
    private SessionClientFactory sessionClientFactory;

    public Okta(Builder builder) {
        this.mConnectionFactory = builder.mConnectionFactory;
        this.mOIDCAccount = builder.mOIDCAccount;
        this.mStorage = builder.mStorage;
        this.authClientFactory = builder.authClientFactory;
        this.sessionClientFactory = builder.sessionClientFactory;
    }

    public <T extends INativeAuthClient> T getBrowserAuthorizationClient() {
        return (T)this.authClientFactory.createBrowser();
    }

    public <T extends INativeAuthClient> T  getNativeAuthorizationClient() {
        return (T)this.authClientFactory.createNative();
    }

    public <T extends ISessionClient> T  getSessionClient() {
        return (T)this.sessionClientFactory.createSession();
    }


    public <T extends IBrowserAuthClient> T getBrowserAuthorizationClient(Class<T> type) {
        return type.cast(this.authClientFactory.createBrowser());
    }

    public <T extends INativeAuthClient> T getNativeAuthorizationClient(Class<T> type) {
        return type.cast(this.authClientFactory.createNative());
    }

    public <T extends ISessionClient> T  getSessionClient(Class<T> type) {
        return type.cast(this.sessionClientFactory.createSession());
    }

    public static class Builder {
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private OktaStorage mStorage;
        private AuthClientFactory authClientFactory;
        private SessionClientFactory sessionClientFactory;

        public Builder() {
        }

        public Okta create() {
            return new Okta(this);
        }

        public Okta.Builder withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        public Okta.Builder withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

        public Okta.Builder withStorage(OktaStorage storage) {
            this.mStorage = storage;
            return this;
        }

        public Okta.Builder withAuthenticationClientFactory(AuthClientFactory authClientFactory) {
            this.authClientFactory = authClientFactory;
            return this;
        }

        public Okta.Builder withSessionClientFactory(SessionClientFactory sessionClientFactory) {
            this.sessionClientFactory = sessionClientFactory;
            return this;
        }
    }
}
