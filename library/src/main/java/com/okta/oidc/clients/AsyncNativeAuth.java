package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

public interface AsyncNativeAuth extends BaseAuth<AsyncSession> {
    /**
     * Log in with a session token. This is for logging in without using the implicit flow.
     * A session token can be obtained by using the Authentication API. For more information
     * about different types of
     * <a href=https://developer.okta.com/authentication-guide/auth-overview/#choosing-an-oauth-2-0-flow>Authentication flows</a>
     *
     * @param sessionToken the session token
     * @param payload      the {@link AuthenticationPayload payload}
     * @param cb           the @{@link RequestCallback callback}
     * @see <a href=https://developer.okta.com/docs/api/resources/authn/>Revoke token</a>
     */
    void logIn(String sessionToken, AuthenticationPayload payload, RequestCallback<AuthorizationResult, AuthorizationException> cb);
}
