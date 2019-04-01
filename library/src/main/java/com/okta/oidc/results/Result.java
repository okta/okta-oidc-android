package com.okta.oidc.results;


import com.okta.oidc.util.AuthorizationException;

public class Result {

    private final AuthorizationException error;

    Result(AuthorizationException error) {
        this.error = error;
    }

    public static Result success() {
        return new Result(null);
    }

    public static Result error(AuthorizationException error) {
        return new Result(error);
    }

    public boolean isSuccess(){
        return getError() == null;
    }

    public AuthorizationException getError() {
        return error;
    }

}
