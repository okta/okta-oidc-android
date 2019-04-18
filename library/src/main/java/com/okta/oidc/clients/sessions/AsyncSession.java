package com.okta.oidc.clients.sessions;

import android.net.Uri;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface AsyncSession {
    void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb);
    void introspectToken(String token, String tokenType,
                         final RequestCallback<IntrospectResponse, AuthorizationException> cb);
    void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb);
    void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb);
    Tokens getTokens();
    void authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                           @Nullable Map<String, String> postParameters,
                           @NonNull HttpConnection.RequestMethod method,
                           final RequestCallback<JSONObject, AuthorizationException> cb);
    boolean isLoggedIn();
    void clear();
}
