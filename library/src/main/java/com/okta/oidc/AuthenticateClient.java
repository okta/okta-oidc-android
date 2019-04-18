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
package com.okta.oidc;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.AnyThread;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.net.HttpConnection;
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
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Client API for Okta OpenID Connect & OAuth 2.0 APIs.
 * <pre>
 * {@code
 * OIDCAccount account = new OIDCAccount.Builder()
 *     .clientId("{clientId}")
 *     .redirectUri("{redirectUri}")
 *     .endSessionRedirectUri("{endSessionUri}")
 *     .scopes("openid", "profile", "offline_access")
 *     .discoveryUri("https://{yourOktaDomain}")
 *     .create();
 *
 * AuthenticateClient client = new AuthenticateClient.Builder()
 *     .withAccount(account)
 *     .withContext(getApplicationContext())
 *     .withStorage(new SimpleOktaStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .create();
 * }
 * </pre>
 * <p>
 * Note that callbacks are executed on the UI thread unless a executor is provided to the builder.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
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

    /**
     * Register a callback for login and logout result status. The callback is triggered when
     * {@link #logIn(FragmentActivity, AuthenticationPayload) logIn} or
     * {@link #signOutFromOkta(FragmentActivity)} signOutFromOkta} is completed.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
     *      @Override
     *      public void onSuccess(@NonNull AuthorizationStatus status) {
     *          if (status == AuthorizationStatus.AUTHORIZED) {
     *              //login success. client can be used to perform protected resource requests.
     *          } else if (status == AuthorizationStatus.LOGGED_OUT) {
     *              //logout success. browser session is cleared.
     *          } else if (status == AuthorizationStatus.IN_PROGRESS) {
     *              //request in progress.
     *          }
     *      }
     *
     *      @Override
     *      public void onCancel() {
     *          //request canceled
     *      }
     *
     *      @Override
     *      public void onError(@NonNull String msg, AuthorizationException error) {
     *          //error
     *      }
     * }, this);
     * </pre>
     * }
     *
     * @param resultCallback returns the result of login or logout attempts.
     * @param activity       the activity which will receive the results.
     */
    public void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException> resultCallback, FragmentActivity activity) {
        mResultCb = resultCallback;
        registerActivityLifeCycle(activity);
        mAuthClient.registerCallbackIfInterrupt(activity, (result, type) -> {
            switch (type) {
                case SIGN_IN:
                    processLogInResult((AuthorizationResult) result);
                    break;
                case SIGN_OUT:
                    processSignOutResult(result);
                    break;
            }
        }, mDispatcher);

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

    /**
     * Get user profile returns any claims for the currently logged-in user.
     * <p>
     * This must be done after the user is logged-in (client has a valid access token).
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.getUserProfile(new RequestCallback<JSONObject, AuthorizationException>() {
     *     @Override
     *     public void onSuccess(@NonNull JSONObject result) {
     *         //handle JSONObject result.
     *     }
     *
     *     @Override
     *     public void onError(String error, AuthorizationException exception) {
     *         //handle failed userinfo request
     *     }
     * });
     *
     * </pre>
     * }
     *
     * @param cb the RequestCallback to be executed when request is finished.
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#userinfo">User info</a>
     */
    public void getUserProfile(final RequestCallback<JSONObject, AuthorizationException> cb) {
        AuthorizedRequest request = mAuthClient.userProfileRequest();
        request.dispatchRequest(mDispatcher, cb);
    }

    /**
     * Revoke token takes an access or refresh token and revokes it. Revoked tokens are considered
     * inactive at the introspection endpoint. A client may only revoke its own tokens.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.revokeToken(client.getTokens().getRefreshToken(),
     *     new RequestCallback<Boolean, AuthorizationException>() {
     *         @Override
     *         public void onSuccess(@NonNull Boolean result) {
     *             //handle result
     *         }
     *         @Override
     *         public void onError(String error, AuthorizationException exception) {
     *             //handle request error
     *         }
     *     });
     * </pre>
     * }
     *
     * @param token the token to be revoked. Can be the access or refresh token.
     * @param cb    the RequestCallback to be executed when request is finished.
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#revoke">Revoke token</a>
     */
    public void revokeToken(String token, final RequestCallback<Boolean, AuthorizationException> cb) {
        RevokeTokenRequest request = mAuthClient.revokeTokenRequest(token);
        request.dispatchRequest(mDispatcher, cb);
    }

    /**
     * Introspect token takes an access, refresh, or ID token, and returns a boolean
     * indicating whether it is active or not. If the token is active, additional data about
     * the token is also returned {@link IntrospectResponse}. If the token is invalid, expired,
     * or revoked, it is considered inactive.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.introspectToken(client.getTokens().getRefreshToken(),
     *     TokenTypeHint.REFRESH_TOKEN, new RequestCallback<IntrospectResponse, AuthorizationException>() {
     *         @Override
     *         public void onSuccess(@NonNull IntrospectResponse result) {
     *             //handle introspect response.
     *         }
     *
     *         @Override
     *         public void onError(String error, AuthorizationException exception) {
     *             //handle request error
     *         }
     *     }
     * );
     * </pre>
     * }
     *
     * @param token     for introspection. Can be the access, refresh or ID token.
     * @param tokenType the type must be of {@link com.okta.oidc.net.params.TokenTypeHint}
     * @param cb        the RequestCallback to be executed when request is finished.
     * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#introspect">Introspect token</a>
     */
    public void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectResponse, AuthorizationException> cb) {
        IntrospectRequest request = mAuthClient.introspectTokenRequest(token, tokenType);
        request.dispatchRequest(mDispatcher, cb);
    }

    /**
     * Performs a custom authorized request with the access token automatically added to the
     * "Authorization" header with the standard OAuth 2.0 prefix of "Bearer".
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.authorizedRequest(uri, properties,
     *                 postParameters, HttpConnection.RequestMethod.POST,
     *                 new RequestCallback<JSONObject, AuthorizationException>() {
     *     @Override
     *     public void onSuccess(@NonNull JSONObject result) {
     *         //handle JSONObject result.
     *     }
     *
     *     @Override
     *     public void onError(String error, AuthorizationException exception) {
     *         //handle failed request
     *     }
     * });
     * </pre>
     * }
     *
     * @param uri            the uri to protected resource
     * @param properties     the query request properties
     * @param postParameters the post parameters
     * @param method         the http method {@link HttpConnection.RequestMethod}
     * @param cb             the RequestCallback to be executed when request is finished.
     */
    public void authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                  @Nullable Map<String, String> postParameters,
                                  @NonNull HttpConnection.RequestMethod method,
                                  final RequestCallback<JSONObject, AuthorizationException> cb) {
        AuthorizedRequest request = mAuthClient.authorizedRequest(uri,
                properties, postParameters, method);
        request.dispatchRequest(mDispatcher, cb);
    }

    /**
     * Refresh token returns access, refresh, and ID tokens {@link Tokens}.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
     *     @Override
     *     public void onSuccess(@NonNull Tokens result) {
     *         //handle success.
     *     }
     *
     *     @Override
     *     public void onError(String error, AuthorizationException exception) {
     *         //handle request failure
     *     }
     * });
     * </pre>
     * }
     *
     * @param cb the RequestCallback to be executed when request is finished.
     */
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

    /**
     * Gets tokens {@link Tokens}.
     *
     * @return the tokens
     */
    public Tokens getTokens() {
        return mAuthClient.getTokens();
    }

    /**
     * Gets authorization status.
     *
     * @return the {@link AuthorizationStatus status}.
     */
    public AuthorizationStatus getAuthorizationStatus() {
        return mAuthClient.getAuthorizationStatus();
    }

    /**
     * Checks to see if the user is logged-in. If the client have a access or ID token then
     * the user is considered logged-in. This does not check the validity of the access token which
     * could be expired or revoked. For more information about the tokens see
     * {@link #introspectToken(String, String, RequestCallback) introspectToken}
     *
     * @return the boolean
     */
    public boolean isLoggedIn() {
        return mAuthClient.isLoggedIn();
    }

    /**
     * Clears all data from {@link AuthenticateClient}. This will remove all tokens from the client.
     * This should be done after {@link #signOutFromOkta(FragmentActivity) signOutFromOkta} to
     * remove the tokens from the device.
     */
    public void clear() {
        mAuthClient.clear();
    }

    /**
     * Log in with a session token. This is for logging in without using the implicit flow.
     * A session token can be obtained by using the Authentication API. For more information
     * about different types of
     * <a href=https://developer.okta.com/authentication-guide/auth-overview/#choosing-an-oauth-2-0-flow>Authentication flows</a>
     * <p>
     * The result will be returned in the {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param sessionToken the session token
     * @param payload      the {@link AuthenticationPayload payload}
     * @see <a href=https://developer.okta.com/docs/api/resources/authn/>Revoke token</a>
     */
    @AnyThread
    public void logIn(String sessionToken, AuthenticationPayload payload) {
        logIn(null, payload, sessionToken);
    }

    /**
     * Log in using implicit flow.
     * <p>
     * The result will be returned in the {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     */
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

    /**
     * Sign out from okta. This will clear the browser session
     * <p>
     * The result will be returned in the {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param activity the activity
     */
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

    /**
     * The type Builder.
     */
    public static class Builder {
        private Executor mCallbackExecutor;
        private HttpConnectionFactory mConnectionFactory;
        private OIDCAccount mOIDCAccount;
        private int mCustomTabColor;
        private OktaStorage mStorage;
        private Context mContext;
        private String[] mSupportedBrowsers;

        /**
         * Instantiates a new Builder
         */
        public Builder() {
        }

        /**
         * Create authenticate client.
         *
         * @return the authenticate client {@link AuthenticateClient}
         */
        public AuthenticateClient create() {
            return new AuthenticateClient(this);
        }

        /**
         * Sets the account used for this client.
         * {@link OIDCAccount}
         *
         * @param account the account
         * @return current builder
         */
        public Builder withAccount(@NonNull OIDCAccount account) {
            mOIDCAccount = account;
            return this;
        }

        /**
         * Sets the color for custom tab.
         *
         * @param customTabColor the custom tab color for the browser
         * @return current builder
         */
        public Builder withTabColor(@ColorInt int customTabColor) {
            mCustomTabColor = customTabColor;
            return this;
        }

        /**
         * Sets a executor for use for callbacks. Default behaviour will execute
         * callbacks on the UI thread.
         *
         * @param executor custom executor
         * @return current builder
         */
        public Builder withCallbackExecutor(Executor executor) {
            mCallbackExecutor = executor;
            return this;
        }

        /**
         * Sets the connection factory to use, which creates a {@link java.net.HttpURLConnection}
         * instance for communication with Okta OIDC endpoints.
         *
         * @param connectionFactory the connection factory
         * @return the builder
         */
        public Builder withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
            mConnectionFactory = connectionFactory;
            return this;
        }

        /**
         * Set a storage implementation for the client to use. You can define your own storage
         * or use the default implementation {@link com.okta.oidc.storage.SimpleOktaStorage}
         *
         * @param storage the storage implementation
         * @return current builder
         */
        public Builder withStorage(OktaStorage storage) {
            this.mStorage = storage;
            return this;
        }


        /**
         * Sets the context.
         *
         * @param context the context
         * @return current builder
         */
        public Builder withContext(Context context) {
            this.mContext = context;
            return this;
        }

        /**
         * Sets the supported browsers to use. The default is Chrome. Can use other
         * custom tab enabled browsers.
         *
         * @param browsers the package name of the browsers.
         * @return current builder
         */
        public Builder supportedBrowsers(String... browsers) {
            mSupportedBrowsers = browsers;
            return this;
        }
    }
}