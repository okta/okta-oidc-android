package com.okta.oidc.clients.natives;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.results.AuthorizationResult;

import androidx.annotation.Nullable;

public interface SyncNativeAuth {
    AuthorizationResult logIn(String sessionToken, @Nullable AuthenticationPayload payload);
}
