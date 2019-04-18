package com.okta.oidc.factory.client.async;

import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.factory.client.INativeAuthClient;

public class NativeAsyncAuthClient implements INativeAuthClient {
    public void loginWithSession(String session, OktaResultFragment.AuthResultListener listener) {
        listener.postResult(OktaResultFragment.Result.canceled(), null);
    }
}
