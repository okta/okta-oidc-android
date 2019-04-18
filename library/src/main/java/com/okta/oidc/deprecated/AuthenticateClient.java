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
package com.okta.oidc.deprecated;

import android.app.Activity;
import android.content.Context;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.EmptyActivityLifeCycle;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public final class AuthenticateClient {
    private static final String TAG = AuthenticateClient.class.getSimpleName();

    private WeakReference<FragmentActivity> mActivity;

    private SyncAuthenticationClient mAuthClient;
    private RequestDispatcher mDispatcher;

    private ResultCallback<AuthorizationStatus, AuthorizationException> mResultCb;

    private AuthenticateClient(@NonNull Builder builder) {
        mAuthClient = new SyncAuthenticationClient(builder.mConnectionFactory, builder.mOIDCAccount,
                builder.mCustomTabColor, builder.mStorage, builder.mContext,
                builder.mSupportedBrowsers);
        mDispatcher = new RequestDispatcher(builder.mCallbackExecutor);
    }



    private void registerActivityLifeCycle(@NonNull final FragmentActivity activity) {
        mActivity = new WeakReference<>(activity);
        mActivity.get().getApplication().registerActivityLifecycleCallbacks(new EmptyActivityLifeCycle() {
            @Override
            public void onActivityDestroyed(Activity activity) {
                if (mActivity != null && mActivity.get() == activity) {
                    stop();
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }
        });
    }

    public void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb) {
        AuthorizedRequest request = mAuthClient.userProfileRequest();
        request.dispatchRequest(mDispatcher, cb);
    }

    public void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb) {
        RevokeTokenRequest request = mAuthClient.revokeTokenRequest(token);
        request.dispatchRequest(mDispatcher, cb);
    }

    public void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectResponse, AuthorizationException> cb) {
        IntrospectRequest request = mAuthClient.introspectTokenRequest(token, tokenType);
        request.dispatchRequest(mDispatcher, cb);
    }

    public void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) {
        //Wrap the callback from the app because we want to be consistent in
        //returning a Tokens object instead of a TokenResponse.
        mAuthClient.refreshTokenRequest().dispatchRequest(mDispatcher,
                new RequestCallback<TokenResponse, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull TokenResponse result) {
                        mAuthClient.mOktaState.save(result);
                        cb.onSuccess(new Tokens(result));
                    }

                    @Override
                    public void onError(String error, AuthorizationException exception) {
                        cb.onError(error, exception);
                    }
                });
    }

    public Tokens getTokens() {
        return mAuthClient.getTokens();
    }

    public AuthorizationStatus getAuthorizationStatus() {
        return mAuthClient.getAuthorizationStatus();
    }

    public boolean isLoggedIn() {
        return mAuthClient.isLoggedIn();
    }

    public void clear() {
        mAuthClient.clear();
    }

    @AnyThread
    public void logIn(String sessionToken, AuthenticationPayload payload) {
        logIn(null, payload, sessionToken);
    }

    @AnyThread
    public void logIn(@NonNull final FragmentActivity activity, AuthenticationPayload payload) {
        logIn(activity, payload, null);
    }

    private void logIn(@Nullable final FragmentActivity activity, AuthenticationPayload payload, String sessionToken) {
        if (activity != null) {
            registerActivityLifeCycle(activity);
        }
        mDispatcher.execute(() -> {
            try {
                AuthorizationResult result;
                if (sessionToken != null) {
                    result = mAuthClient.logInNative(sessionToken, payload);
                } else {
                    result = mAuthClient.logIn(activity, payload);
                }
                processLogInResult(result);
            } catch (InterruptedException e) {
                mDispatcher.submitResults(() -> {
                    if (mResultCb != null) {
                        mResultCb.onCancel();
                    }
                });
            }
        });
    }

    private void processLogInResult(AuthorizationResult result) {
        if (result.isSuccess()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onSuccess(
                            AuthorizationStatus.AUTHORIZED);
                }
            });
        } else if (result.isCancel()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onCancel();
                }
            });
        } else {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onError("Authorization error",
                            result.getError());
                }
            });
        }
    }

    @AnyThread
    public void signOutFromOkta(@NonNull final FragmentActivity activity) {
        registerActivityLifeCycle(activity);
        mDispatcher.execute(() -> {
            try {
                Result result = mAuthClient.signOutFromOkta(activity);
                processSignOutResult(result);
            } catch (InterruptedException e) {
                mDispatcher.submitResults(() -> {
                    if (mResultCb != null) {
                        mResultCb.onCancel();
                    }
                });
            }
        });
    }

    private void processSignOutResult(Result result) {
        if (result.isSuccess()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onSuccess(
                            AuthorizationStatus.LOGGED_OUT);
                }
            });
        } else if (result.isCancel()) {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onCancel();
                }
            });
        } else {
            mDispatcher.submitResults(() -> {
                if (mResultCb != null) {
                    mResultCb.onError("Log out error",
                            result.getError());
                }
            });
        }
    }

    private void stop() {
        mResultCb = null;
        mDispatcher.shutdown();
    }

    public static class Builder {
        private Executor mCallbackExecutor;
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private int mCustomTabColor;
        private OktaStorage mStorage;
        private Context mContext;
        private String[] mSupportedBrowsers;

        public Builder() {
        }

        public AuthenticateClient create() {
            return new AuthenticateClient(this);
        }

        public Builder withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        public Builder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        public Builder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        public Builder withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

        public Builder withStorage(OktaStorage storage) {
            this.mStorage = storage;
            return this;
        }


        public Builder withContext(Context context) {
            this.mContext = context;
            return this;
        }

        public Builder supportedBrowsers(String... browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }
    }


}