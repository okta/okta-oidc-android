package com.okta.oidc.results;


import com.okta.oidc.Tokens;
import com.okta.oidc.util.AuthorizationException;

public class AuthorizationResult extends Result {

    private final Tokens tokens;

    public static AuthorizationResult success(Tokens tokens) {
        return new AuthorizationResult(null, tokens);
    }

    public static AuthorizationResult error(AuthorizationException error) {
        return new AuthorizationResult(error, null);
    }

    AuthorizationResult(AuthorizationException error, Tokens tokens) {
        super(error);
        this.tokens = tokens;
    }

    public Tokens getTokens() {
        return tokens;
    }
}
