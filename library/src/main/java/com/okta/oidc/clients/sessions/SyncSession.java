package com.okta.oidc.clients.sessions;

import android.net.Uri;

import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public interface SyncSession {
    AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters,
                                               @NonNull HttpConnection.RequestMethod method);
    UserInfo getUserProfile() throws AuthorizationException;
    IntrospectInfo introspectToken(String token, String tokenType) throws AuthorizationException;
    Boolean revokeToken(String token) throws AuthorizationException;
    Tokens refreshToken() throws AuthorizationException;
    Tokens getTokens();
    boolean isLoggedIn();
    void clear();
}
