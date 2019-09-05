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

package com.okta.oidc.example;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputEditText;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sample to test library functionality. Can be used as a starting reference point.
 */
@SuppressLint("SetTextI18n")
@SuppressWarnings("FieldCanBeLocal")
public class SampleActivity extends AppCompatActivity implements SignInDialog.SignInDialogListener {
    private static final String TAG = "SampleActivity";
    private static final String PREF_SWITCH = "switch";
    private static final String PREF_NON_WEB = "nonweb";
    /**
     * Authorization client using chrome custom tab as a user agent.
     */
    WebAuthClient mWebAuth;
    /**
     * Authorization client used with Authentication APIs sessionToken.
     */
    AuthClient mAuthClient;
    /**
     * The authorized client to interact with Okta's endpoints.
     */
    SessionClient mSessionClient;

    /**
     * Okta OIDC configuration.
     */
    @VisibleForTesting
    OIDCConfig mOidcConfig;

    OIDCConfig mOAuth2Config;
    WebAuthClient mWebOAuth2;
    SessionClient mSessionOAuth2Client;
    SessionClient mSessionNonWebClient;

    private TextView mTvStatus;
    private Button mSignInBrowser;
    private Button mSignInNative;
    private Button mSignOut;
    private Button mGetProfile;
    private Button mClearData;

    private Button mRefreshToken;
    private Button mRevokeRefresh;
    private Button mRevokeAccess;
    private Button mIntrospectRefresh;
    private Button mIntrospectAccess;
    private Button mIntrospectId;
    private Button mCheckExpired;
    private Button mCancel;
    private Switch mSwitch;
    private ProgressBar mProgressBar;
    private boolean mIsSessionSignIn;
    @SuppressWarnings("unused")
    private static final String FIRE_FOX = "org.mozilla.firefox";

    private LinearLayout mRevokeContainer;

    private TextInputEditText mEditText;

    /**
     * The payload to send for authorization.
     */
    @VisibleForTesting
    AuthenticationPayload mPayload;

    /**
     * The payload to send for authorization.
     */
    @VisibleForTesting
    SharedPreferenceStorage mStorageOidc;
    @VisibleForTesting
    SharedPreferenceStorage mStorageOAuth2;

    /**
     * The Authentication API client.
     */
    protected AuthenticationClient mAuthenticationClient;
    private SignInDialog mSignInDialog;
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sample_activity);
        mCancel = findViewById(R.id.cancel);
        mCheckExpired = findViewById(R.id.check_expired);
        mSignInBrowser = findViewById(R.id.sign_in);
        mSignInNative = findViewById(R.id.sign_in_native);
        mSignOut = findViewById(R.id.sign_out);
        mClearData = findViewById(R.id.clear_data);
        mRevokeContainer = findViewById(R.id.revoke_token);
        mRevokeAccess = findViewById(R.id.revoke_access);
        mRevokeRefresh = findViewById(R.id.revoke_refresh);
        mRefreshToken = findViewById(R.id.refresh_token);
        mGetProfile = findViewById(R.id.get_profile);
        mProgressBar = findViewById(R.id.progress_horizontal);
        mTvStatus = findViewById(R.id.status);
        mIntrospectRefresh = findViewById(R.id.introspect_refresh);
        mIntrospectAccess = findViewById(R.id.introspect_access);
        mIntrospectId = findViewById(R.id.introspect_id);
        mSwitch = findViewById(R.id.switch1);

        mEditText = findViewById(R.id.login_hint);

        mStorageOAuth2 = new SharedPreferenceStorage(this, "OAUTH2");
        mStorageOidc = new SharedPreferenceStorage(this);
        boolean checked = getSharedPreferences(SampleActivity.class.getName(), MODE_PRIVATE)
                .getBoolean(PREF_SWITCH, true);
        mIsSessionSignIn = getSharedPreferences(SampleActivity.class.getName(), MODE_PRIVATE)
                .getBoolean(PREF_NON_WEB, true);

        mSwitch.setChecked(checked);
        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setupCallback();//reset callbacks
            if (getSessionClient().isAuthenticated()) {
                showAuthenticatedMode();
            } else {
                showSignedOutMode();
            }
            mSwitch.setText(isChecked ? "OIDC" : "OAuth2");
        });

        mCheckExpired.setOnClickListener(v -> {
            SessionClient client = getSessionClient();
            try {
                mTvStatus.setText(client.getTokens().isAccessTokenExpired() ? "token expired" :
                        "token not expired");
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mIntrospectRefresh.setOnClickListener(v -> {
            showNetworkProgress(true);
            SessionClient client = getSessionClient();
            String refreshToken;
            try {
                refreshToken = client.getTokens().getRefreshToken();
                client.introspectToken(refreshToken, TokenTypeHint.REFRESH_TOKEN,
                        new RequestCallback<IntrospectInfo, AuthorizationException>() {
                            @Override
                            public void onSuccess(@NonNull IntrospectInfo result) {
                                mTvStatus.setText("RefreshToken active: " + result.isActive());
                                mProgressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(String error, AuthorizationException exception) {
                                mTvStatus.setText("RefreshToken Introspect error");
                                mProgressBar.setVisibility(View.GONE);
                            }
                        }
                );
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mIntrospectAccess.setOnClickListener(v -> {
            showNetworkProgress(true);
            SessionClient client = getSessionClient();
            try {
                client.introspectToken(
                        client.getTokens().getAccessToken(), TokenTypeHint.ACCESS_TOKEN,
                        new RequestCallback<IntrospectInfo, AuthorizationException>() {
                            @Override
                            public void onSuccess(@NonNull IntrospectInfo result) {
                                mTvStatus.setText("AccessToken active: " + result.isActive());
                                mProgressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(String error, AuthorizationException exception) {
                                mTvStatus.setText("AccessToken Introspect error");
                                mProgressBar.setVisibility(View.GONE);
                            }
                        }
                );
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mIntrospectId.setOnClickListener(v -> {
            showNetworkProgress(true);
            SessionClient client = getSessionClient();
            try {
                client.introspectToken(
                        client.getTokens().getIdToken(), TokenTypeHint.ID_TOKEN,
                        new RequestCallback<IntrospectInfo, AuthorizationException>() {
                            @Override
                            public void onSuccess(@NonNull IntrospectInfo result) {
                                mTvStatus.setText("IdToken active: " + result.isActive());
                                mProgressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(String error, AuthorizationException exception) {
                                mTvStatus.setText("IdToken Introspect error");
                                mProgressBar.setVisibility(View.GONE);
                            }
                        }
                );
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mGetProfile.setOnClickListener(v -> getProfile());
        mRefreshToken.setOnClickListener(v -> {
            showNetworkProgress(true);
            SessionClient client = getSessionClient();
            client.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
                @Override
                public void onSuccess(@NonNull Tokens result) {
                    mTvStatus.setText("token refreshed");
                    showNetworkProgress(false);
                }

                @Override
                public void onError(String error, AuthorizationException exception) {
                    mTvStatus.setText(exception.errorDescription);
                    showNetworkProgress(false);
                }
            });
        });

        mRevokeRefresh.setOnClickListener(v -> {
            SessionClient client = getSessionClient();
            try {
                Tokens tokens = client.getTokens();
                if (tokens != null && tokens.getRefreshToken() != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    client.revokeToken(client.getTokens().getRefreshToken(),
                            new RequestCallback<Boolean, AuthorizationException>() {
                                @Override
                                public void onSuccess(@NonNull Boolean result) {

                                    String status = "Revoke refresh token : " + result;
                                    Log.d(TAG, status);
                                    mTvStatus.setText(status);
                                    mProgressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onError(String error,
                                                    AuthorizationException exception) {
                                    Log.d(TAG, exception.error +
                                            " revokeRefreshToken onError " + error, exception);
                                    mTvStatus.setText(error);
                                    mProgressBar.setVisibility(View.GONE);
                                }
                            });
                }
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mRevokeAccess.setOnClickListener(v -> {
            SessionClient client = getSessionClient();
            try {
                Tokens tokens = client.getTokens();
                if (tokens != null && tokens.getAccessToken() != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    client.revokeToken(client.getTokens().getAccessToken(),
                            new RequestCallback<Boolean, AuthorizationException>() {
                                @Override
                                public void onSuccess(@NonNull Boolean result) {
                                    String status = "Revoke Access token : " + result;
                                    Log.d(TAG, status);
                                    mTvStatus.setText(status);
                                    mProgressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onError(String error,
                                                    AuthorizationException exception) {
                                    Log.d(TAG, exception.error +
                                            " revokeAccessToken onError " + error, exception);
                                    mTvStatus.setText(error);
                                    mProgressBar.setVisibility(View.GONE);
                                }
                            });
                }
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mSignOut.setOnClickListener(v -> {
            showNetworkProgress(true);
            WebAuthClient client = getWebAuthClient();
            client.signOutOfOkta(this);
        });
        mClearData.setOnClickListener(v -> {
            SessionClient client = getSessionClient();
            client.clear();
            mTvStatus.setText("clear data");
            showSignedOutMode();
        });

        mSignInBrowser.setOnClickListener(v -> {
            showNetworkProgress(true);
            WebAuthClient client = getWebAuthClient();
            String loginHint = mEditText.getEditableText().toString();
            if (!TextUtils.isEmpty(loginHint)) {
                mPayload = new AuthenticationPayload.Builder()
                        .setLoginHint(loginHint)
                        .build();
            }
            client.signIn(this, mPayload);
        });

        mSignInNative.setOnClickListener(v -> {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("signin");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            mSignInDialog = new SignInDialog();
            mSignInDialog.setListener(this);
            mSignInDialog.show(ft, "signin");
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mAuthenticationClient = AuthenticationClients.builder()
                    .setOrgUrl(BuildConfig.DISCOVERY_URI)
                    .build();
        } else {
            mSignInNative.setVisibility(View.GONE);
        }

        //Example of using JSON file to create config
        mOidcConfig = new OIDCConfig.Builder()
                .withJsonFile(this, R.raw.okta_oidc_config)
                .create();

        //Example of config
        mOidcConfig = new OIDCConfig.Builder()
                .clientId(BuildConfig.CLIENT_ID)
                .redirectUri(BuildConfig.REDIRECT_URI)
                .endSessionRedirectUri(BuildConfig.END_SESSION_URI)
                .scopes(BuildConfig.SCOPES)
                .discoveryUri(BuildConfig.DISCOVERY_URI)
                .create();

        mOAuth2Config = new OIDCConfig.Builder()
                .clientId(BuildConfig.CLIENT_ID)
                .redirectUri(BuildConfig.REDIRECT_URI)
                .endSessionRedirectUri(BuildConfig.END_SESSION_URI)
                .scopes(BuildConfig.SCOPES)
                .discoveryUri(BuildConfig.DISCOVERY_URI + "/oauth2/default")
                .create();

        //use custom connection factory
        MyConnectionFactory factory = new MyConnectionFactory();
        factory.setClientType(MyConnectionFactory.USE_SYNC_OK_HTTP);

        boolean isEmulator = isEmulator();
        mWebOAuth2 = new Okta.WebAuthBuilder()
                .withConfig(mOAuth2Config)
                .withContext(getApplicationContext())
                .withStorage(mStorageOAuth2)
                .withCallbackExecutor(null)
                .withEncryptionManager(new DefaultEncryptionManager(this))
                .setRequireHardwareBackedKeyStore(!isEmulator)
                .withTabColor(0)
                .supportedBrowsers(FIRE_FOX)
                .create();

        mSessionOAuth2Client = mWebOAuth2.getSessionClient();

        Okta.WebAuthBuilder builder = new Okta.WebAuthBuilder()
                .withConfig(mOidcConfig)
                .withContext(getApplicationContext())
                .withStorage(mStorageOidc)
                .withCallbackExecutor(null)
                .withEncryptionManager(new DefaultEncryptionManager(this))
                .setRequireHardwareBackedKeyStore(!isEmulator)
                .withTabColor(0)
                .withOktaHttpClient(factory.build())
                .supportedBrowsers(FIRE_FOX);

        mWebAuth = builder.create();

        mSessionClient = mWebAuth.getSessionClient();

        mAuthClient = new Okta.AuthBuilder()
                .withConfig(mOidcConfig)
                .withContext(getApplicationContext())
                .withStorage(new SharedPreferenceStorage(this))
                .withEncryptionManager(new DefaultEncryptionManager(this))
                .setRequireHardwareBackedKeyStore(false)
                .withCallbackExecutor(null)
                .create();

        mSessionNonWebClient = mAuthClient.getSessionClient();

        if (getSessionClient().isAuthenticated()) {
            showAuthenticatedMode();
        }

        mCancel.setOnClickListener(v -> {
            getWebAuthClient().cancel(); //cancel web auth requests
            getSessionClient().cancel(); //cancel session requests
            showNetworkProgress(false);
        });
        setupCallback();
    }

    /**
     * Sets callback.
     */
    @VisibleForTesting
    void setupCallback() {
        ResultCallback<AuthorizationStatus, AuthorizationException> callback =
                new ResultCallback<AuthorizationStatus, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull AuthorizationStatus status) {
                        Log.d("SampleActivity", "AUTHORIZED");
                        if (status == AuthorizationStatus.AUTHORIZED) {
                            mTvStatus.setText("authentication authorized");
                            showAuthenticatedMode();
                            showNetworkProgress(false);
                            mIsSessionSignIn = false;
                            mProgressBar.setVisibility(View.GONE);
                        } else if (status == AuthorizationStatus.SIGNED_OUT) {
                            //this only clears the session.
                            mTvStatus.setText("signedOutOfOkta");
                            showNetworkProgress(false);
                        }
                    }

                    @Override
                    public void onCancel() {
                        mProgressBar.setVisibility(View.GONE);
                        Log.d(TAG, "CANCELED!");
                        mTvStatus.setText("canceled");
                    }

                    @Override
                    public void onError(@Nullable String msg, AuthorizationException error) {
                        mProgressBar.setVisibility(View.GONE);
                        Log.d("SampleActivity", error.error +
                                " onActivityResult onError " + msg, error);
                        mTvStatus.setText(msg);
                    }
                };
        if (mSwitch.isChecked()) {
            mWebAuth.registerCallback(callback, this);
        } else {
            mWebOAuth2.registerCallback(callback, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebAuth.isInProgress()) {
            showNetworkProgress(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        showNetworkProgress(false);
        getSharedPreferences(SampleActivity.class.getName(), MODE_PRIVATE).edit()
                .putBoolean(PREF_SWITCH, mSwitch.isChecked()).apply();
        getSharedPreferences(SampleActivity.class.getName(), MODE_PRIVATE).edit()
                .putBoolean(PREF_NON_WEB, mIsSessionSignIn).apply();

    }

    private SessionClient getSessionClient() {

        if (mIsSessionSignIn) {
            return mSessionNonWebClient;
        }
        return mSwitch.isChecked() ? mSessionClient : mSessionOAuth2Client;
    }

    private WebAuthClient getWebAuthClient() {
        return mSwitch.isChecked() ? mWebAuth : mWebOAuth2;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
        if (mSignInDialog != null && mSignInDialog.isVisible()) {
            mSignInDialog.dismiss();
        }
    }

    private void showNetworkProgress(boolean visible) {
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        mCancel.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void showAuthenticatedMode() {
        mGetProfile.setVisibility(View.VISIBLE);
        mSignOut.setVisibility(View.VISIBLE);
        mClearData.setVisibility(View.VISIBLE);
        mRefreshToken.setVisibility(View.VISIBLE);
        mRevokeContainer.setVisibility(View.VISIBLE);
        mSignInBrowser.setVisibility(View.GONE);
        mSignInNative.setVisibility(View.GONE);
    }

    private void showSignedOutMode() {
        mSignInBrowser.setVisibility(View.VISIBLE);
        if (mAuthenticationClient != null) {
            mSignInNative.setVisibility(View.VISIBLE);
        } else {
            mSignInNative.setVisibility(View.GONE);
        }
        mGetProfile.setVisibility(View.GONE);
        mSignOut.setVisibility(View.GONE);
        mRefreshToken.setVisibility(View.GONE);
        mClearData.setVisibility(View.GONE);
        mRevokeContainer.setVisibility(View.GONE);
        mTvStatus.setText("");
    }

    private void getProfile() {
        showNetworkProgress(true);
        SessionClient client = getSessionClient();
        client.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull UserInfo result) {
                mTvStatus.setText(result.toString());
                showNetworkProgress(false);
            }

            @Override
            public void onError(String error, AuthorizationException exception) {
                Log.d(TAG, error, exception.getCause());
                mTvStatus.setText("Error : " + exception.errorDescription);
                showNetworkProgress(false);
            }
        });
    }

    @Override
    public void onSignIn(String username, String password) {
        mSignInDialog.dismiss();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            mTvStatus.setText("Invalid username or password");
            return;
        }
        showNetworkProgress(true);
        mExecutor.submit(() -> {
            try {
                if (mAuthenticationClient == null) {
                    return;
                }
                mAuthenticationClient.authenticate(username, password.toCharArray(),
                        null, new AuthenticationStateHandlerAdapter() {
                            @Override
                            public void handleUnknown(
                                    AuthenticationResponse authenticationResponse) {
                                SampleActivity.this.runOnUiThread(() -> {
                                    showNetworkProgress(false);
                                    mTvStatus.setText(authenticationResponse.getStatus().name());
                                });
                            }

                            @Override
                            public void handleLockedOut(AuthenticationResponse lockedOut) {
                                SampleActivity.this.runOnUiThread(() -> {
                                    showNetworkProgress(false);
                                    mTvStatus.setText("Account locked out");
                                });
                            }

                            @Override
                            public void handleSuccess(AuthenticationResponse successResponse) {
                                String sessionToken = successResponse.getSessionToken();
                                mAuthClient.signIn(sessionToken, mPayload,
                                        new RequestCallback<Result,
                                                AuthorizationException>() {
                                            @Override
                                            public void onSuccess(
                                                    @NonNull Result result) {
                                                mTvStatus.setText("authentication authorized");
                                                mIsSessionSignIn = true;
                                                showAuthenticatedMode();
                                                showNetworkProgress(false);
                                            }

                                            @Override
                                            public void onError(String error,
                                                                AuthorizationException exception) {
                                                mTvStatus.setText(error);
                                            }
                                        });
                            }
                        });
            } catch (AuthenticationException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                runOnUiThread(() -> {
                    showNetworkProgress(false);
                    mTvStatus.setText(e.getMessage());
                });
            }
        });
    }

    /**
     * Check if the device is a emulator.
     *
     * @return true if it is emulator
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Google")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.DEVICE.contains("generic");
    }
}
