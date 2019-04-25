package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.clients.sessions.SyncSession;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;


public interface SyncWebAuth extends BaseAuth<SyncSession> {
    boolean isInProgress();

    /**
     * Log in using implicit flow.
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     * @return the result
     */
    AuthorizationResult logIn(@NonNull final FragmentActivity activity, @Nullable AuthenticationPayload payload)
            throws InterruptedException;

    /**
     * Sign out from okta. This will clear the browser session
     *
     * @param activity the activity
     * @return the result
     */
    Result signOutFromOkta(@NonNull final FragmentActivity activity)
            throws InterruptedException;

}
