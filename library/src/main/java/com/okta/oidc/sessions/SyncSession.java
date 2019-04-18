package com.okta.oidc.sessions;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;


public interface SyncSession {
    JSONObject getUserProfile() throws AuthorizationException;
    IntrospectResponse introspectToken(String token, String tokenType) throws AuthorizationException;
    Boolean revokeToken(String token) throws AuthorizationException;
    Tokens refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) throws AuthorizationException;
    Tokens getTokens();
    boolean isLoggedIn();
    void clear();
}
