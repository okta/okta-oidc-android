package com.okta.oidc.clients.webs;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.OktaRedirectActivity;
import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.OktaState;
import com.okta.oidc.State;
import com.okta.oidc.deprecated.SyncAuthenticationClient;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.LogoutRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;

import static com.okta.oidc.State.IDLE;
import static com.okta.oidc.util.AuthorizationException.RegistrationRequestErrors.INVALID_REDIRECT_URI;

class SyncWebAuthClient extends AuthClient implements SyncWebAuth {
    private static final String TAG = SyncWebAuthClient.class.getSimpleName();

    private OIDCAccount mOIDCAccount;
    private String[] mSupportedBrowsers;
    private int mCustomTabColor;

    public SyncWebAuthClient(OIDCAccount mOIDCAccount,
                             OktaState mOktaState,
                             HttpConnectionFactory mConnectionFactory,
                             String[] mSupportedBrowsers,
                             int mCustomTabColor) {
        super(mOIDCAccount, mOktaState, mConnectionFactory);
        this.mOIDCAccount = mOIDCAccount;
        this.mSupportedBrowsers = mSupportedBrowsers;
        this.mCustomTabColor = mCustomTabColor;
    }

    private boolean isRedirectUrisRegistered(@NonNull Uri uri, FragmentActivity activity) {
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> resolveInfos = null;
        if (pm != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(uri);
            resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        }
        boolean found = false;
        if (resolveInfos != null) {
            for (ResolveInfo info : resolveInfos) {
                ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo.name.equals(OktaRedirectActivity.class.getCanonicalName()) &&
                        activityInfo.packageName.equals(activity.getPackageName())) {
                    found = true;
                } else {
                    Log.w(TAG, "Warning! Multiple " +
                            "applications found registered with same scheme");
                    //Another installed app have same url scheme.
                    //return false as if no activity found to prevent hijacking of redirect.
                    return false;
                }
            }
        }
        return found;
    }

    protected void registerCallbackIfInterrupt(FragmentActivity activity, SyncAuthenticationClient.ResultListener resultListener, ExecutorService executorService) {
        if (OktaResultFragment.hasRequestInProgress(activity)) {
            OktaResultFragment.getFragment(activity).setAuthenticationListener((result, type) -> {
                executorService.execute(() -> {
                    switch (type) {
                        case SIGN_IN:
                            AuthorizationResult authorizationResult = processLogInResult(result);
                            resetCurrentState();
                            if(resultListener != null)
                                resultListener.postResult(authorizationResult, type);
                            break;
                        case SIGN_OUT:
                            Result signOutResult = processSignOutResult(result);
                            resetCurrentState();
                            if(resultListener != null)
                                resultListener.postResult(signOutResult, type);
                            break;
                    }
                });
            });
        }
    }

    protected void unRegisterCallback(FragmentActivity activity) {
        if (OktaResultFragment.hasRequestInProgress(activity)) {
            OktaResultFragment.getFragment(activity).setAuthenticationListener(null);
        }
    }

    @Override
    public boolean isInProgress() {
        return mOktaState.getCurrentState() != IDLE;
    }

    @Override
    @WorkerThread
    public AuthorizationResult logIn(@NonNull final FragmentActivity activity,
                                     @Nullable AuthenticationPayload payload)
            throws InterruptedException {
        try {
            obtainNewConfiguration();
        } catch (AuthorizationException e) {
            resetCurrentState();
            return AuthorizationResult.error(e);
        }

        WebRequest request = new AuthorizeRequest.Builder()
                .account(mOIDCAccount)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .authenticationPayload(payload)
                .create();

        mOktaState.save(request);
        if (!isRedirectUrisRegistered(mOIDCAccount.getRedirectUri(), activity)) {
            Log.e(TAG, "No uri registered to handle redirect or multiple applications registered");
            //FIXME move error to listener
            resetCurrentState();
            return AuthorizationResult.error(INVALID_REDIRECT_URI);
        }
        mOktaState.setCurrentState(State.SIGN_IN_REQUEST);
        AtomicReference<OktaResultFragment.Result> resultWrapper = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        OktaResultFragment.addLoginFragment(request, mCustomTabColor,
                activity, (result, type) -> {
                    resultWrapper.set(result);
                    latch.countDown();
                }, mSupportedBrowsers);
        latch.await();
        OktaResultFragment.Result authResult = resultWrapper.get();
        AuthorizationResult result = processLogInResult(authResult);
        resetCurrentState();
        if (result == null) {
            throw new IllegalStateException("login performed in illegal states");
        }

        return result;
    }

    private AuthorizationResult processLogInResult(OktaResultFragment.Result result) {
        switch (result.getStatus()) {
            case CANCELED:
                return AuthorizationResult.cancel();
            case ERROR:
                return AuthorizationResult.error(result.getException());
            case AUTHORIZED:
                mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
                TokenResponse response;
                try {
                    validateResult(result.getAuthorizationResponse());
                    response = tokenExchange(
                            (AuthorizeResponse) result.getAuthorizationResponse())
                            .executeRequest();
                } catch (AuthorizationException e) {
                    return AuthorizationResult.error(e);
                }
                mOktaState.save(response);
                return AuthorizationResult.success(new Tokens(response));
        }
        return null;
    }

    @Override
    @AnyThread
    public Result signOutFromOkta(@NonNull final FragmentActivity activity)
            throws InterruptedException {
//        if (isLoggedIn()) {
            mOktaState.setCurrentState(State.SIGN_OUT_REQUEST);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OktaResultFragment.Result> resultWrapper = new AtomicReference<>();
            WebRequest request = new LogoutRequest.Builder()
                    .provideConfiguration(mOktaState.getProviderConfiguration())
                    .account(mOIDCAccount)
                    .tokenResponse(mOktaState.getTokenResponse())
                    .state(CodeVerifierUtil.generateRandomState())
                    .create();
            mOktaState.save(request);
            OktaResultFragment.addLogoutFragment(request, mCustomTabColor,
                    activity, (result, type) -> {
                        resultWrapper.set(result);
                        latch.countDown();
                    }, mSupportedBrowsers);
            latch.await();
            OktaResultFragment.Result logoutResult = resultWrapper.get();
            Result result = processSignOutResult(logoutResult);
            resetCurrentState();
            if (result != null) {
                return result;
            } else {
                return Result.error(AuthorizationException.AuthorizationRequestErrors.OTHER);
            }

//        }
//        resetCurrentState();
//        return Result.esuccess();
    }

    private Result processSignOutResult(OktaResultFragment.Result result) {
        switch (result.getStatus()) {
            case CANCELED:
                return Result.error(INVALID_REDIRECT_URI);
            case ERROR:
                return Result.error(result.getException());
            case LOGGED_OUT:
                return Result.success();
        }
        return null;
    }
}
