package com.okta.oidc.deprecated;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.clients.AuthClientFactory;
import com.okta.oidc.sessions.SessionClientFactory;
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

public class OktaTemp {

    private HttpConnectionFactory mConnectionFactory;
    private OIDCAccount mOIDCAccount;
    private OktaStorage mStorage;
    private AuthClientFactory authClientFactory;
    private SessionClientFactory sessionClientFactory;

    public OktaTemp(Builder builder) {
        this.mConnectionFactory = builder.mConnectionFactory;
        this.mOIDCAccount = builder.mOIDCAccount;
        this.mStorage = builder.mStorage;
        this.authClientFactory = builder.authClientFactory;
        this.sessionClientFactory = builder.sessionClientFactory;
    }


//    public <T extends ISessionClient> T  getSessionClient() {
//        return (T)this.sessionClientFactory.createSession();
//    }
//
//
//
//    public <T extends ISessionClient> T  getSessionClient(Class<T> type) {
//        return type.cast(this.sessionClientFactory.createSession(this.mOIDCAccount, ));
//    }

    public static class Builder {
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private OktaStorage mStorage;
        private AuthClientFactory authClientFactory;
        private SessionClientFactory sessionClientFactory;

        public Builder() {
        }

        public OktaTemp create() {
            return new OktaTemp(this);
        }

        public OktaTemp.Builder withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        public OktaTemp.Builder withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

        public OktaTemp.Builder withStorage(OktaStorage storage) {
            this.mStorage = storage;
            return this;
        }

        public OktaTemp.Builder withAuthenticationClientFactory(AuthClientFactory authClientFactory) {
            this.authClientFactory = authClientFactory;
            return this;
        }

        public OktaTemp.Builder withSessionClientFactory(SessionClientFactory sessionClientFactory) {
            this.sessionClientFactory = sessionClientFactory;
            return this;
        }
    }
}
