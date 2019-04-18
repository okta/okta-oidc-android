package com.okta.oidc.factory.client.async;

import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.factory.client.IBrowserAuthClient;

public class BrowserAsyncAuthClient implements IBrowserAuthClient {
    public void login(OktaResultFragment.AuthResultListener listener) {
        listener.postResult(OktaResultFragment.Result.canceled(), null);
    }
    public void singOutFromOkta(OktaResultFragment.AuthResultListener listener) {
        listener.postResult(OktaResultFragment.Result.canceled(), null);
    }
}
