package com.okta.oidc.sessions;

import android.net.Uri;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class AsyncSessionClient implements AsyncSession {
    SyncSessionClient mSyncSessionClient;
    private RequestDispatcher mDispatcher;

    AsyncSessionClient(Executor callbackExecutor, OIDCAccount oidcAccount, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        mSyncSessionClient = new SyncSessionClient(oidcAccount, oktaState, connectionFactory);
        mDispatcher = new RequestDispatcher(callbackExecutor);
    }

    public void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb) {
        AuthorizedRequest request = mSyncSessionClient.userProfileRequest();
        request.dispatchRequest(mDispatcher, cb);
    }

    public void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectResponse, AuthorizationException> cb) {
        IntrospectRequest request = mSyncSessionClient.introspectTokenRequest(token, tokenType);
        request.dispatchRequest(mDispatcher, cb);
    }

    public void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb) {
        RevokeTokenRequest request = mSyncSessionClient.revokeTokenRequest(token);
        request.dispatchRequest(mDispatcher, cb);
    }

    public void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) {
        //Wrap the callback from the app because we want to be consistent in
        //returning a Tokens object instead of a TokenResponse.
        mSyncSessionClient.refreshTokenRequest().dispatchRequest(mDispatcher,
                new RequestCallback<TokenResponse, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull TokenResponse result) {
                        mSyncSessionClient.mOktaState.save(result);
                        cb.onSuccess(new Tokens(result));
                    }

                    @Override
                    public void onError(String error, AuthorizationException exception) {
                        cb.onError(error, exception);
                    }
                });
    }

    public Tokens getTokens() {
        return mSyncSessionClient.getTokens();
    }

    @Override
    public AuthorizedRequest authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties, @Nullable Map<String, String> postParameters, @NonNull HttpConnection.RequestMethod method) {
        return mSyncSessionClient.authorizedRequest(uri, properties, postParameters, method);
    }

    public boolean isLoggedIn() {
        return mSyncSessionClient.isLoggedIn();
    }

    public void clear() {
        mSyncSessionClient.clear();
    }
}
