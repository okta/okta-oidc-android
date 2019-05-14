/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.okta.oidc.clients.web;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaRedirectActivity;
import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.AuthAPI;
import com.okta.oidc.clients.State;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.clients.sessions.SyncSessionClientFactoryImpl;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.LogoutRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.Result;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.clients.State.IDLE;
import static com.okta.oidc.util.AuthorizationException.RegistrationRequestErrors.INVALID_REDIRECT_URI;

class SyncWebAuthClientImpl extends AuthAPI implements SyncWebAuthClient {
    private static final String TAG = SyncWebAuthClientImpl.class.getSimpleName();

    private String[] mSupportedBrowsers;
    private int mCustomTabColor;
    private SyncSessionClient mSessionClient;

    SyncWebAuthClientImpl(OIDCConfig oidcConfig,
                          OktaState oktaState,
                          HttpConnectionFactory connectionFactory,
                          int customTabColor,
                          String... supportedBrowsers) {
        super(oidcConfig, oktaState, connectionFactory);
        mSupportedBrowsers = supportedBrowsers;
        mCustomTabColor = customTabColor;
        mSessionClient = new SyncSessionClientFactoryImpl()
                .createClient(oidcConfig, oktaState, connectionFactory);
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

    public void registerCallbackIfInterrupt(FragmentActivity activity,
                                            ResultListener resultListener,
                                            ExecutorService executorService) {
        if (OktaResultFragment.hasRequestInProgress(activity)) {
            OktaResultFragment.getFragment(activity).setAuthenticationListener((result, type)
                    -> {
                if (!executorService.isShutdown()) {
                    executorService.execute(() -> {
                        switch (type) {
                            case SIGN_IN:
                                Result authorizationResult =
                                        processSignInResult(result);
                                resetCurrentState();
                                if (resultListener != null) {
                                    resultListener.postResult(authorizationResult, type);
                                }
                                break;
                            case SIGN_OUT:
                                Result signOutResult = processSignOutResult(result);
                                resetCurrentState();
                                if (resultListener != null) {
                                    resultListener.postResult(signOutResult, type);
                                }
                                break;
                            default:
                                break;
                        }
                    });
                }
            });
        }
    }

    public void unregisterCallback(FragmentActivity activity) {
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
    public Result signIn(@NonNull final FragmentActivity activity,
                         @Nullable AuthenticationPayload payload)
            throws InterruptedException {
        try {
            obtainNewConfiguration();
        } catch (AuthorizationException e) {
            resetCurrentState();
            return Result.error(e);
        }

        WebRequest request = new AuthorizeRequest.Builder()
                .config(mOidcConfig)
                .providerConfiguration(mOktaState.getProviderConfiguration())
                .authenticationPayload(payload)
                .create();

        mOktaState.save(request);
        if (!isRedirectUrisRegistered(mOidcConfig.getRedirectUri(), activity)) {
            Log.e(TAG, "No uri registered to handle redirect " +
                    "or multiple applications registered");
            //FIXME move error to listener
            resetCurrentState();
            return Result.error(INVALID_REDIRECT_URI);
        }
        mOktaState.setCurrentState(State.SIGN_IN_REQUEST);
        AtomicReference<OktaResultFragment.StateResult> resultWrapper = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        OktaResultFragment.addLoginFragment(request, mCustomTabColor,
                activity, (result, type) -> {
                    resultWrapper.set(result);
                    latch.countDown();
                }, mSupportedBrowsers);
        latch.await();
        OktaResultFragment.StateResult authResult = resultWrapper.get();
        Result result = processSignInResult(authResult);
        resetCurrentState();
        if (result == null) {
            throw new IllegalStateException("sign in performed in illegal states");
        }

        return result;
    }

    private Result processSignInResult(OktaResultFragment.StateResult result) {
        switch (result.getStatus()) {
            case CANCELED:
                return Result.cancel();
            case ERROR:
                return Result.error(result.getException());
            case AUTHORIZED:
                mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
                TokenResponse response;
                try {
                    validateResult(result.getAuthorizationResponse());
                    response = tokenExchange(
                            (AuthorizeResponse) result.getAuthorizationResponse())
                            .executeRequest();
                } catch (AuthorizationException e) {
                    return Result.error(e);
                }
                mOktaState.save(response);
                return Result.success();
            default:
                return null;
        }
    }

    @Override
    @AnyThread
    public Result signOutOfOkta(@NonNull final FragmentActivity activity)
            throws InterruptedException {
        mOktaState.setCurrentState(State.SIGN_OUT_REQUEST);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<OktaResultFragment.StateResult> resultWrapper = new AtomicReference<>();
        WebRequest request = new LogoutRequest.Builder()
                .provideConfiguration(mOktaState.getProviderConfiguration())
                .config(mOidcConfig)
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
        OktaResultFragment.StateResult logoutResult = resultWrapper.get();
        Result result = processSignOutResult(logoutResult);
        resetCurrentState();
        if (result != null) {
            return result;
        } else {
            return Result.error(AuthorizationException.AuthorizationRequestErrors.OTHER);
        }
    }

    private Result processSignOutResult(OktaResultFragment.StateResult result) {
        switch (result.getStatus()) {
            case CANCELED:
                return Result.error(INVALID_REDIRECT_URI);
            case ERROR:
                return Result.error(result.getException());
            case LOGGED_OUT:
                return Result.success();
            default:
                return null;
        }
    }

    public interface ResultListener {
        void postResult(Result result, OktaResultFragment.ResultType resultType);
    }

    @Override
    public SyncSessionClient getSessionClient() {
        return this.mSessionClient;
    }
}
