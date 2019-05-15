package com.okta.oidc.clients.sessions;

import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SessionClientFactoryImpl {
    private Executor executor;

    public SessionClientFactoryImpl(Executor executor) {
        this.executor = executor;
    }

    public SessionClient createClient(SyncSessionClient syncSessionClient) {
        return new SessionClientImpl(executor, syncSessionClient);
    }
}
