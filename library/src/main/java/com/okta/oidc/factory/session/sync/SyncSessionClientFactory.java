package com.okta.oidc.factory.session.sync;

import com.okta.oidc.factory.session.SessionClientFactory;

public class SyncSessionClientFactory extends SessionClientFactory<SyncSessionClient> {

    @Override
    public SyncSessionClient createSession() {
        return new SyncSessionClient();
    }
}
