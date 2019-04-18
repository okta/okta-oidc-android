package com.okta.oidc.sessions;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import static com.okta.oidc.State.IDLE;

class SyncSessionClient implements SyncSession {
    private OIDCAccount mOIDCAccount;
    OktaState mOktaState;
    private HttpConnectionFactory mConnectionFactory;

    SyncSessionClient(OIDCAccount oidcAccount, OktaState oktaState, HttpConnectionFactory connectionFactory) {
        this.mOIDCAccount = oidcAccount;
        this.mOktaState = oktaState;
        this.mConnectionFactory = connectionFactory;
    }


    protected AuthorizedRequest userProfileRequest() {
        return (AuthorizedRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.PROFILE)
                .connectionFactory(mConnectionFactory)
                .tokenResponse(mOktaState.getTokenResponse())
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    @Override
    public JSONObject getUserProfile() throws AuthorizationException {
        return userProfileRequest().executeRequest();
    }

    protected IntrospectRequest introspectTokenRequest(String token, String tokenType) {
        return (IntrospectRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.INTROSPECT)
                .connectionFactory(mConnectionFactory)
                .introspect(token, tokenType)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    @Override
    public IntrospectResponse introspectToken(String token, String tokenType) throws AuthorizationException {
        return introspectTokenRequest(token, tokenType).executeRequest();
    }

    public RevokeTokenRequest revokeTokenRequest(String token) {
        return (RevokeTokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REVOKE_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenToRevoke(token)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    @Override
    public Boolean revokeToken(String token) throws AuthorizationException {
        return revokeTokenRequest(token).executeRequest();
    }

    public RefreshTokenRequest refreshTokenRequest() {
        return (RefreshTokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.REFRESH_TOKEN)
                .connectionFactory(mConnectionFactory)
                .tokenResponse(mOktaState.getTokenResponse())
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .account(mOIDCAccount).createRequest();
    }

    @Override
    public Tokens refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) throws AuthorizationException {
        //Wrap the callback from the app because we want to be consistent in
        //returning a Tokens object instead of a TokenResponse.
        TokenResponse tokenResponse = refreshTokenRequest().executeRequest();
        mOktaState.save(tokenResponse);
        return new Tokens(tokenResponse);
    }

    @Override
    public Tokens getTokens() {
        TokenResponse response = mOktaState.getTokenResponse();
        if (response == null) return null;
        return new Tokens(response);
    }

    @Override
    public boolean isLoggedIn() {
        TokenResponse tokenResponse = mOktaState.getTokenResponse();
        return tokenResponse != null &&
                (tokenResponse.getAccessToken() != null || tokenResponse.getIdToken() != null);
    }

    @Override
    public void clear() {
        mOktaState.delete(mOktaState.getProviderConfiguration());
        mOktaState.delete(mOktaState.getTokenResponse());
        mOktaState.delete(mOktaState.getAuthorizeRequest());
        mOktaState.setCurrentState(IDLE);
    }
}
