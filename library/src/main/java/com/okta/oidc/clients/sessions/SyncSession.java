package com.okta.oidc.clients.sessions;

import android.net.Uri;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public interface SyncSession {
    AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters,
                                               @NonNull HttpConnection.RequestMethod method);
    JSONObject getUserProfile() throws AuthorizationException;
    IntrospectResponse introspectToken(String token, String tokenType) throws AuthorizationException;
    Boolean revokeToken(String token) throws AuthorizationException;
    Tokens refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) throws AuthorizationException;
    Tokens getTokens();
    boolean isLoggedIn();
    void clear();
}
