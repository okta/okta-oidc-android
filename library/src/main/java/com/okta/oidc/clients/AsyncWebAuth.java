package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.util.AuthorizationException;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface AsyncWebAuth extends BaseAuth<AsyncSession> {
    boolean isInProgress();
    void logIn(@NonNull final FragmentActivity activity, AuthenticationPayload payload);
    void signOutFromOkta(@NonNull final FragmentActivity activity);
    void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException> resultCallback, FragmentActivity activity);
    void unregisterCallback();
}
