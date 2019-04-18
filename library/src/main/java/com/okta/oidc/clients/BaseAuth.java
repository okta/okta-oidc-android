package com.okta.oidc.clients;

public interface BaseAuth<S> {
    S getSessionClient();
}
