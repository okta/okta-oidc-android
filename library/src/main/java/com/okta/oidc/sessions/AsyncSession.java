package com.okta.oidc.sessions;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

public interface AsyncSession {
    void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb);
    void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectResponse, AuthorizationException> cb);
    void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb);
    void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb);
    Tokens getTokens();
    boolean isLoggedIn();
    void clear();
}
