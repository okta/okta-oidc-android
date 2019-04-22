package com.okta.oidc.sessions;

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

public interface AsyncSession {
    //TODO: add callback
    void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb);
    void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectResponse, AuthorizationException> cb);
    void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb);
    void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb);
    Tokens getTokens();
    AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters,
                                        @NonNull HttpConnection.RequestMethod method);
    boolean isLoggedIn();
    void clear();
}
