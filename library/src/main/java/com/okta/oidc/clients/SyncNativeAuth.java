package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.clients.sessions.SyncSession;
import com.okta.oidc.results.AuthorizationResult;

import androidx.annotation.Nullable;

public interface SyncNativeAuth extends BaseAuth<SyncSession> {
    AuthorizationResult logIn(String sessionToken, @Nullable AuthenticationPayload payload);
}
