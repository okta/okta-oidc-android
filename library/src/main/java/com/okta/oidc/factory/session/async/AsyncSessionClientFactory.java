package com.okta.oidc.factory.session.async;

import com.okta.oidc.factory.session.SessionClientFactory;

public class AsyncSessionClientFactory extends SessionClientFactory<AsyncSessionClient> {
    @Override
    public AsyncSessionClient createSession() {
        return new AsyncSessionClient();
    }
}
