package com.okta.oidc.factory.client.sync;

import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.factory.client.IBrowserAuthClient;

public class BrowserSyncAuthClient implements IBrowserAuthClient {
    public OktaResultFragment.Result login() {
        return OktaResultFragment.Result.authorized(null);
    }
    public OktaResultFragment.Result singOutFromOkta() {
        return OktaResultFragment.Result.authorized(null);
    }

}
