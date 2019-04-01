package com.okta.oidc;

import android.support.annotation.RestrictTo;

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.Persistable;

@RestrictTo(RestrictTo.Scope.LIBRARY)
class OktaState {
    private OktaRepository mOktaRepo;

    OktaState(OktaRepository mOktaRepository) {
        this.mOktaRepo = mOktaRepository;
    }

    TokenResponse getTokenResponse() {
        return this.mOktaRepo.get(TokenResponse.RESTORE);
    }

    ProviderConfiguration getProviderConfiguration() {
        return this.mOktaRepo.get(ProviderConfiguration.RESTORE);
    }

    WebRequest getAuthorizeRequest() {
        return this.mOktaRepo.get(WebRequest.RESTORE);
    }

    void save(Persistable persistable) {
        this.mOktaRepo.save(persistable);
    }

    void delete(Persistable persistable) {
        this.mOktaRepo.delete(persistable);
    }
}
