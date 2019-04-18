package com.okta.oidc.factory.client.sync;

import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.factory.client.INativeAuthClient;

public class NativeSyncAuthClient  implements INativeAuthClient {
    public OktaResultFragment.Result loginWithSession(String session) {
        return OktaResultFragment.Result.authorized(null);
    }
}
