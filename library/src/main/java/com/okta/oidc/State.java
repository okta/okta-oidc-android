package com.okta.oidc;

public enum State {
    IDLE,
    OBTAIN_CONFIGURATION,
    SIGN_IN_REQUEST,
    SIGN_OUT_REQUEST,
    TOKEN_EXCHANGE
}
