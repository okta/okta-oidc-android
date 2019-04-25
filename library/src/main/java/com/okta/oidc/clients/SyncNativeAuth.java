package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.clients.sessions.SyncSession;
import com.okta.oidc.results.AuthorizationResult;

import androidx.annotation.Nullable;

public interface SyncNativeAuth extends BaseAuth<SyncSession> {
    /**
     * Log in with a session token. This is for logging in without using the implicit flow.
     * A session token can be obtained by using the Authentication API. For more information
     * about different types of
     * <a href=https://developer.okta.com/authentication-guide/auth-overview/#choosing-an-oauth-2-0-flow>Authentication flows</a>
     *
     * @param sessionToken the session token
     * @param payload      the {@link AuthenticationPayload payload}
     * @return the {@link AuthorizationResult authorizationResult}
     */
    AuthorizationResult logIn(String sessionToken, @Nullable AuthenticationPayload payload);
}
