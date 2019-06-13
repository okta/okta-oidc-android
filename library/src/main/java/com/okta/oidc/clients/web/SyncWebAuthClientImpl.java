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

import android.app.Activity;
import android.content.Context;
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
import com.okta.oidc.AuthenticationResultHandler;
import com.okta.oidc.AuthenticationResultHandler.AuthResultListener;
import com.okta.oidc.AuthenticationResultHandler.StateResult;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaRedirectActivity;
import com.okta.oidc.clients.AuthAPI;
import com.okta.oidc.clients.State;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.clients.sessions.SyncSessionClientFactoryImpl;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.LogoutRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.AuthorizationException.EncryptionErrors;
import com.okta.oidc.util.CodeVerifierUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.AuthenticationResultHandler.ResultType;
import static com.okta.oidc.OktaResultFragment.REQUEST_CODE_SIGN_IN;
import static com.okta.oidc.OktaResultFragment.REQUEST_CODE_SIGN_OUT;
import static com.okta.oidc.OktaResultFragment.addLoginFragment;
import static com.okta.oidc.OktaResultFragment.addLogoutFragment;
import static com.okta.oidc.OktaResultFragment.createAuthIntent;
import static com.okta.oidc.clients.State.IDLE;
import static com.okta.oidc.util.AuthorizationException.RegistrationRequestErrors.INVALID_REDIRECT_URI;
import static com.okta.oidc.util.AuthorizationException.TYPE_OAUTH_REGISTRATION_ERROR;

class SyncWebAuthClientImpl extends AuthAPI implements SyncWebAuthClient {
    private static final String TAG = SyncWebAuthClientImpl.class.getSimpleName();

    private String[] mSupportedBrowsers;
    private int mCustomTabColor;
    private SyncSessionClient mSessionClient;
    private AuthenticationResultHandler mHandler;

    SyncWebAuthClientImpl(OIDCConfig oidcConfig,
                          Context context,
                          OktaStorage oktaStorage,
                          EncryptionManager encryptionManager,
                          OktaHttpClient httpClient,
                          boolean requireHardwareBackedKeyStore,
                          boolean cacheMode,
                          int customTabColor,
                          String... supportedBrowsers) {
        super(oidcConfig, context, oktaStorage, encryptionManager, requireHardwareBackedKeyStore,
                cacheMode);
        mSupportedBrowsers = supportedBrowsers;
        mCustomTabColor = customTabColor;
        mHttpClient = httpClient;
        mSessionClient = new SyncSessionClientFactoryImpl()
                .createClient(oidcConfig, mOktaState, mHttpClient);
        mHandler = AuthenticationResultHandler.handler();
    }

    private boolean isRedirectUrisRegistered(@NonNull Uri uri, Activity activity) {
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

    @Override
    public void registerCallbackIfInterrupt(Activity activity, ResultListener resultListener,
                                            ExecutorService executorService) {
        AuthResultListener authResultListener = (result, type) -> {
            if (!executorService.isShutdown()) {
                executorService.execute(() -> {
                    switch (type) {
                        case SIGN_IN:
                            Result authorizationResult = processSignInResult(result);
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
        };
        mHandler.setAuthenticationListener(authResultListener);
    }

    @Override
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        mHandler.onActivityResult(requestCode, resultCode, data);
    }

    public void unregisterCallback() {
        mHandler.setAuthenticationListener(null);
    }

    @Override
    public boolean isInProgress() {
        return mOktaState.getCurrentState() != IDLE;
    }

    private StateResult startSignIn(Activity activity, WebRequest request)
            throws InterruptedException {
        AtomicReference<StateResult> resultWrapper = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        if (activity instanceof FragmentActivity) {
            addLoginFragment(request, mCustomTabColor, (FragmentActivity) activity,
                    mSupportedBrowsers);
        } else {
            Intent intent = createAuthIntent(activity, request.toUri(), mCustomTabColor,
                    mSupportedBrowsers);
            activity.startActivityForResult(intent, REQUEST_CODE_SIGN_IN);
        }
        mHandler.setAuthenticationListener((result, type) -> {
            resultWrapper.set(result);
            latch.countDown();
        });
        latch.await();
        return resultWrapper.get();
    }

    @Override
    @WorkerThread
    public Result signIn(@NonNull final Activity activity,
                         @Nullable AuthenticationPayload payload) {
        mCancel.set(false);

        try {
            if (!isRedirectUrisRegistered(mOidcConfig.getRedirectUri(), activity)) {
                String errorDescription = "No uri registered to handle redirect " +
                        "or multiple applications registered";
                Log.e(TAG, errorDescription);
                throw new AuthorizationException(
                        TYPE_OAUTH_REGISTRATION_ERROR, INVALID_REDIRECT_URI.code,
                        INVALID_REDIRECT_URI.error, errorDescription, null, null);
            }

            ProviderConfiguration configuration = obtainNewConfiguration();
            checkIfCanceled();
            WebRequest request = new AuthorizeRequest.Builder()
                    .config(mOidcConfig)
                    .providerConfiguration(configuration)
                    .authenticationPayload(payload)
                    .create();
            mOktaState.save(request);
            mOktaState.setCurrentState(State.SIGN_IN_REQUEST);

            StateResult authResult = startSignIn(activity, request);
            return processSignInResult(authResult);
        } catch (AuthorizationException e) {
            return Result.error(e);
        } catch (IOException | InterruptedException e) {
            return Result.cancel();
        } catch (OktaRepository.EncryptionException e) {
            return Result.error(EncryptionErrors.byEncryptionException(e));
        } finally {
            resetCurrentState();
        }
    }

    @NonNull
    private Result processSignInResult(StateResult result) {
        if (result == null) {
            return Result.error(new AuthorizationException("Result is empty",
                    new NullPointerException()));
        }
        switch (result.getStatus()) {
            case CANCELED:
                return Result.cancel();
            case ERROR:
                return Result.error(result.getException());
            case AUTHORIZED:
                mOktaState.setCurrentState(State.TOKEN_EXCHANGE);
                TokenResponse response;
                try {
                    WebRequest authorizedRequest = mOktaState.getAuthorizeRequest();
                    ProviderConfiguration providerConfiguration =
                            mOktaState.getProviderConfiguration();
                    validateResult(result.getAuthorizationResponse(), authorizedRequest);
                    TokenRequest request = tokenExchange(
                            (AuthorizeResponse) result.getAuthorizationResponse(),
                            providerConfiguration,
                            (AuthorizeRequest) authorizedRequest);
                    mCurrentRequest.set(new WeakReference<>(request));
                    response = request.executeRequest(mHttpClient);
                    mOktaState.save(response);
                } catch (OktaRepository.EncryptionException e) {
                    return Result.error(EncryptionErrors.byEncryptionException(e));
                } catch (AuthorizationException e) {
                    return Result.error(e);
                }
                return Result.success();
            default:
                return Result.error(new AuthorizationException("StateResult with invalid status: "
                        + result.getStatus().name(), new IllegalStateException()));
        }
    }

    private StateResult startSignOut(Activity activity, WebRequest request)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<StateResult> resultWrapper = new AtomicReference<>();
        if (activity instanceof FragmentActivity) {
            addLogoutFragment(request, mCustomTabColor, (FragmentActivity) activity,
                    mSupportedBrowsers);
        } else {
            Intent intent = createAuthIntent(activity, request.toUri(),
                    mCustomTabColor, mSupportedBrowsers);
            activity.startActivityForResult(intent, REQUEST_CODE_SIGN_OUT);
        }
        mHandler.setAuthenticationListener((result, type) -> {
            resultWrapper.set(result);
            latch.countDown();
        });

        latch.await();
        return resultWrapper.get();
    }

    @Override
    @AnyThread
    public Result signOutOfOkta(@NonNull final Activity activity) {
        try {
            mOktaState.setCurrentState(State.SIGN_OUT_REQUEST);
            WebRequest request;
            request = new LogoutRequest.Builder()
                    .provideConfiguration(mOktaState.getProviderConfiguration())
                    .config(mOidcConfig)
                    .tokenResponse(mOktaState.getTokenResponse())
                    .state(CodeVerifierUtil.generateRandomState())
                    .create();
            mOktaState.save(request);
            StateResult logoutResult = startSignOut(activity, request);
            return processSignOutResult(logoutResult);
        } catch (InterruptedException e) {
            return Result.cancel();
        } catch (OktaRepository.EncryptionException e) {
            return Result.error(EncryptionErrors.byEncryptionException(e));
        } catch (AuthorizationException e) {
            return Result.error(e);
        } finally {
            resetCurrentState();
        }
    }

    @NonNull
    private Result processSignOutResult(StateResult result) {
        if (result == null) {
            return Result.error(new AuthorizationException("Result is empty",
                    new NullPointerException()));
        }
        switch (result.getStatus()) {
            case CANCELED:
                return Result.error(INVALID_REDIRECT_URI);
            case ERROR:
                return Result.error(result.getException());
            case LOGGED_OUT:
                return Result.success();
            default:
                return Result.error(new AuthorizationException("StateResult with invalid status: "
                        + result.getStatus().name(), new IllegalStateException()));
        }
    }

    public interface ResultListener {
        void postResult(Result result, ResultType resultType);
    }

    @Override
    public void migrateTo(EncryptionManager manager) throws AuthorizationException {
        this.mSessionClient.migrateTo(manager);
    }

    @Override
    public SyncSessionClient getSessionClient() {
        return this.mSessionClient;
    }
}
