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
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.GuardedEncryptionManager;
import com.okta.oidc.util.AuthorizationException;

/**
 * For testing call back path for regular activity.
 */
@SuppressLint("SetTextI18n")
@SuppressWarnings("FieldCanBeLocal")
public class PlainActivity extends Activity {
    private static final String TAG = "PlainActivity";
    /**
     * Authorization client using chrome custom tab as a user agent.
     */
    WebAuthClient mWebAuth;
    /**
     * The authorized client to interact with Okta's endpoints.
     */
    SessionClient mSessionClient;

    /**
     * Okta OIDC configuration.
     */
    @VisibleForTesting
    OIDCConfig mOidcConfig;

    private static final String PREF_FINGERPRINT = "fingerprint";
    private TextView mTvStatus;
    private Button mSignOut;
    private Button mGetProfile;
    private Button mClearData;
    private Button mSignInBrowser;
    private Button mRefreshToken;
    private Button mRevokeRefresh;
    private Button mRevokeAccess;
    private Button mIntrospectRefresh;
    private Button mIntrospectAccess;
    private Button mIntrospectId;
    private Button mCheckExpired;
    private Button mCancel;
    private CheckBox mBiometric;

    private ProgressBar mProgressBar;
    @SuppressWarnings("unused")
    private static final String FIRE_FOX = "org.mozilla.firefox";

    private LinearLayout mRevokeContainer;
    private EncryptionManager mCurrentEncryptionManager;
    private DefaultEncryptionManager mDefaultEncryptionManager;
    private GuardedEncryptionManager mKeyguardEncryptionManager;
    private static final int REQUEST_CODE_CREDENTIALS = 1000;
    /**
     * The payload to send for authorization.
     */
    @VisibleForTesting
    AuthenticationPayload mPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.plain_activity);
        mCancel = findViewById(R.id.cancel);
        mSignInBrowser = findViewById(R.id.sign_in);
        mCheckExpired = findViewById(R.id.check_expired);
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
        mBiometric = findViewById(R.id.biometric);

        mSignInBrowser.setOnClickListener(v -> {
            showNetworkProgress(true);
            mWebAuth.signIn(this, mPayload);
        });

        boolean checked = getSharedPreferences(PlainActivity.class.getName(), MODE_PRIVATE)
                .getBoolean(PREF_FINGERPRINT, false);
        mKeyguardEncryptionManager = new GuardedEncryptionManager(this, Integer.MAX_VALUE);
        mDefaultEncryptionManager = new DefaultEncryptionManager(this);
        mCurrentEncryptionManager = checked ? mKeyguardEncryptionManager :
                mDefaultEncryptionManager;

        mBiometric.setChecked(checked);
        mBiometric.setOnCheckedChangeListener((button, isChecked) -> {
            if (!isKeyguardSecure()) {
                button.setChecked(false);
                mTvStatus.setText("Keyguard not secure. Set a PIN or enroll a fingerprint.");
                return;
            }
            if (isChecked) {
                try {
                    if (!mKeyguardEncryptionManager.isValidKeys()) {
                        mKeyguardEncryptionManager.recreateKeys(this);
                    }
                    mKeyguardEncryptionManager.recreateCipher();
                    mSessionClient.migrateTo(mKeyguardEncryptionManager);
                    mCurrentEncryptionManager = mKeyguardEncryptionManager;
                } catch (AuthorizationException e) {
                    mTvStatus.setText("Error in data migration check logs for error");
                    Log.d(TAG, "Error migrateTo", e);
                }
            } else {
                mCurrentEncryptionManager.removeKeys();
                mSessionClient.clear();
                mCurrentEncryptionManager = mDefaultEncryptionManager;

                try {
                    //set the encryption manager back to default.
                    mSessionClient.migrateTo(mCurrentEncryptionManager);
                } catch (AuthorizationException e) {
                    //NO-OP
                }
                showSignedOutMode();
            }
            getSharedPreferences(PlainActivity.class.getName(), MODE_PRIVATE).edit()
                    .putBoolean(PREF_FINGERPRINT, isChecked).apply();

        });

        mCheckExpired.setOnClickListener(v -> {
            try {
                mTvStatus.setText(mSessionClient.getTokens().isAccessTokenExpired()
                        ? "token expired" : "token not expired");
            } catch (AuthorizationException e) {
                Log.d(TAG, "", e);
            }
        });

        mIntrospectRefresh.setOnClickListener(v -> {
            showNetworkProgress(true);
            String refreshToken;
            try {
                refreshToken = mSessionClient.getTokens().getRefreshToken();
                mSessionClient.introspectToken(refreshToken, TokenTypeHint.REFRESH_TOKEN,
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
            try {
                mSessionClient.introspectToken(
                        mSessionClient.getTokens().getAccessToken(), TokenTypeHint.ACCESS_TOKEN,
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
            try {
                mSessionClient.introspectToken(
                        mSessionClient.getTokens().getIdToken(), TokenTypeHint.ID_TOKEN,
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

        mGetProfile.setOnClickListener(v -> {
            getProfile();
        });
        mRefreshToken.setOnClickListener(v -> {
            showNetworkProgress(true);
            mSessionClient.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
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
            try {
                Tokens tokens = mSessionClient.getTokens();
                if (tokens != null && tokens.getRefreshToken() != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mSessionClient.revokeToken(mSessionClient.getTokens().getRefreshToken(),
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

            try {
                Tokens tokens = mSessionClient.getTokens();
                if (tokens != null && tokens.getAccessToken() != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mSessionClient.revokeToken(mSessionClient.getTokens().getAccessToken(),
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

            mWebAuth.signOutOfOkta(this);
        });
        mClearData.setOnClickListener(v -> {

            mSessionClient.clear();
            mTvStatus.setText("clear data");
            showSignedOutMode();
        });

        mSignInBrowser.setOnClickListener(v -> {
            showNetworkProgress(true);
            mWebAuth.signIn(this, mPayload);
        });

        //Example of config
        mOidcConfig = new OIDCConfig.Builder()
                .clientId("0oajqehiy6p81NVzA0h7")
                .redirectUri("com.oktapreview.samples-test:/callback")
                .endSessionRedirectUri("com.oktapreview.samples-test:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://samples-test.oktapreview.com")
                .create();

        //use custom connection factory
        MyConnectionFactory factory = new MyConnectionFactory();
        factory.setClientType(MyConnectionFactory.USE_SYNC_OK_HTTP);

        boolean isEmulator = isEmulator();

        mWebAuth = new Okta.WebAuthBuilder()
                .withConfig(mOidcConfig)
                .withContext(getApplicationContext())
                .withCallbackExecutor(null)
                .withEncryptionManager(mCurrentEncryptionManager)
                .setRequireHardwareBackedKeyStore(!isEmulator)
                .withTabColor(0)
                .withOktaHttpClient(factory.build())
                .supportedBrowsers(FIRE_FOX)
                .create();

        mSessionClient = mWebAuth.getSessionClient();

        if (mSessionClient.isAuthenticated()) {
            showAuthenticatedMode();
        }

        mCancel.setOnClickListener(v -> {
            mWebAuth.cancel();
            mSessionClient.cancel();
            showNetworkProgress(false);
        });
        setupCallback();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CREDENTIALS && resultCode == RESULT_OK) {
            if (mCurrentEncryptionManager.getCipher() == null) {
                mCurrentEncryptionManager.recreateCipher();
            }
            mTvStatus.setText("Device authenticated");
        } else {
            mWebAuth.handleActivityResult(requestCode, resultCode, data);
        }
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
                        Log.d("PlainActivity", "AUTHORIZED");
                        if (status == AuthorizationStatus.AUTHORIZED) {
                            mTvStatus.setText("authentication authorized");
                            showAuthenticatedMode();
                            showNetworkProgress(false);
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
                        Log.d("PlainActivity", error.error +
                                " onError " + msg, error);
                        mTvStatus.setText(msg);
                    }
                };
        mWebAuth.registerCallback(callback, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebAuth.isInProgress()) {
            showNetworkProgress(true);
        }
        if (mBiometric.isChecked() && !mCurrentEncryptionManager.isUserAuthenticatedOnDevice()) {
            showKeyguard();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        showNetworkProgress(false);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    }

    private void showSignedOutMode() {
        mSignInBrowser.setVisibility(View.VISIBLE);
        mGetProfile.setVisibility(View.GONE);
        mSignOut.setVisibility(View.GONE);
        mRefreshToken.setVisibility(View.GONE);
        mClearData.setVisibility(View.GONE);
        mRevokeContainer.setVisibility(View.GONE);
        mTvStatus.setText("");
    }

    private void getProfile() {
        showNetworkProgress(true);
        mSessionClient.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
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

    /**
     * Check if device have enabled keyguard.
     *
     * @return the boolean
     */
    @VisibleForTesting
    public boolean isKeyguardSecure() {
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    private void showKeyguard() {
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            intent = keyguardManager.createConfirmDeviceCredentialIntent("Confirm credentials", "");
        }
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CREDENTIALS);
        }
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
