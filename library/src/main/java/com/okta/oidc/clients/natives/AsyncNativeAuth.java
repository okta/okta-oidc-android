package com.okta.oidc.clients.natives;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

public interface AsyncNativeAuth {
    void logIn(String sessionToken, AuthenticationPayload payload, RequestCallback<AuthorizationResult, AuthorizationException> cb);
}
