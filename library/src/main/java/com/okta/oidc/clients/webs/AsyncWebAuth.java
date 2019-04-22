package com.okta.oidc.clients.webs;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.util.AuthorizationException;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface AsyncWebAuth {
    public boolean isInProgress();
    public void logIn(@NonNull final FragmentActivity activity, AuthenticationPayload payload);
    public void signOutFromOkta(@NonNull final FragmentActivity activity);
    public void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException> resultCallback, FragmentActivity activity);
    public void unregisterCallback();

}
