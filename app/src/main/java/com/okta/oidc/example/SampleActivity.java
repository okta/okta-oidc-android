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
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.storage.security.FingerprintUtils;
import com.okta.oidc.storage.security.SmartLockEncryptionManager;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.crypto.Cipher;

/**
 * Sample to test library functionality. Can be used as a starting reference point.
 */
@SuppressLint("SetTextI18n")
@SuppressWarnings("FieldCanBeLocal")
public class SampleActivity extends AppCompatActivity implements LoginDialog.LoginDialogListener {
    private static final String TAG = "SampleActivity";
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

    private SmartLockEncryptionManager mEncryptionManager;

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

    private ProgressBar mProgressBar;
    @SuppressWarnings("unused")
    private static final String FIRE_FOX = "org.mozilla.firefox";

    private LinearLayout mRevokeContainer;

    /**
     * The payload to send for authorization.
     */
    @VisibleForTesting
    AuthenticationPayload mPayload;

    /**
     * The Authentication API client.
     */
    protected AuthenticationClient mAuthenticationClient;
    private LoginDialog mLoginDialog;
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private FingerprintHelper mFingerprintHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sample_activity_login);
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

        mIntrospectRefresh.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            String refreshToken = mSessionClient.getTokens().getRefreshToken();
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
        });

        mIntrospectAccess.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
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
        });

        mIntrospectId.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
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
        });

        mGetProfile.setOnClickListener(v -> getProfile());
        mRefreshToken.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            mSessionClient.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
                @Override
                public void onSuccess(@NonNull Tokens result) {
                    mTvStatus.setText("token refreshed");
                    mProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError(String error, AuthorizationException exception) {
                    mTvStatus.setText(exception.errorDescription);
                    mProgressBar.setVisibility(View.GONE);
                }
            });
        });

        mRevokeRefresh.setOnClickListener(v -> {
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
                            public void onError(String error, AuthorizationException exception) {
                                Log.d(TAG, exception.error +
                                        " revokeRefreshToken onError " + error, exception);
                                mTvStatus.setText(error);
                                mProgressBar.setVisibility(View.GONE);
                            }
                        });
            }
        });

        mRevokeAccess.setOnClickListener(v -> {
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
                            public void onError(String error, AuthorizationException exception) {
                                Log.d(TAG, exception.error +
                                        " revokeAccessToken onError " + error, exception);
                                mTvStatus.setText(error);
                                mProgressBar.setVisibility(View.GONE);
                            }
                        });
            }
        });

        mSignOut.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            mWebAuth.signOutFromOkta(this);
        });
        mClearData.setOnClickListener(v -> {
            mSessionClient.clear();
            mTvStatus.setText("clear data");
            showLoggedOutMode();
        });

        mSignInBrowser.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            mWebAuth.logIn(this, mPayload);
        });

        mSignInNative.setOnClickListener(v -> {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("login");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            mLoginDialog = new LoginDialog();
            mLoginDialog.setListener(this);
            mLoginDialog.show(ft, "login");
        });

        mAuthenticationClient = AuthenticationClients.builder()
                .setOrgUrl("https://samples-test.oktapreview.com")
                .build();

        //Example of using JSON file to create config
        mOidcConfig = new OIDCConfig.Builder()
                .withJsonFile(this, R.raw.okta_oidc_config)
                .create();

        //Example of config
        mOidcConfig = new OIDCConfig.Builder()
                .clientId("0oajqehiy6p81NVzA0h7")
                .redirectUri("com.oktapreview.samples-test:/callback")
                .endSessionRedirectUri("com.oktapreview.samples-test:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://samples-test.oktapreview.com")
                .create();

        Okta.WebAuthBuilder builder = new Okta.WebAuthBuilder()
                .withConfig(mOidcConfig)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withCallbackExecutor(null)
                .withTabColor(0)
                .supportedBrowsers(FIRE_FOX);

        if (FingerprintUtils.checkFingerprintCompatibility(this)) {
            mEncryptionManager = new SmartLockEncryptionManager();
            builder.withEncryptionManager(mEncryptionManager);
        }

        mWebAuth = builder.create();

        mSessionClient = mWebAuth.getSessionClient();

        mAuthClient = new Okta.AuthBuilder()
                .withConfig(mOidcConfig)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withCallbackExecutor(null)
                .create();

        if (mSessionClient.isLoggedIn()) {
            showAuthorizedMode();
        }

        setupCallback();
    }

    /**
     * Sets callback.
     */
    @VisibleForTesting
    void setupCallback() {
        mWebAuth.registerCallback(
                new ResultCallback<AuthorizationStatus, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull AuthorizationStatus status) {
                        Log.d("SampleActivity", "AUTHORIZED");
                        if (status == AuthorizationStatus.AUTHORIZED) {
                            mTvStatus.setText("authentication authorized");
                            showAuthorizedMode();
                            mProgressBar.setVisibility(View.GONE);
                        } else if (status == AuthorizationStatus.LOGGED_OUT) {
                            //this only clears the session.
                            mTvStatus.setText("signedOutFromOkta");
                            mProgressBar.setVisibility(View.GONE);
                        } else if (status == AuthorizationStatus.IN_PROGRESS) {
                            mTvStatus.setText("in progress");
                            mProgressBar.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "CANCELED!");
                        mTvStatus.setText("canceled");
                    }

                    @Override
                    public void onError(@Nullable String msg, AuthorizationException error) {
                        Log.d("SampleActivity", error.error +
                                " onActivityResult onError " + msg, error);
                        mTvStatus.setText(msg);
                    }
                }, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebAuth.isInProgress()) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        if (mEncryptionManager != null) {
            prepareSensor();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mProgressBar.setVisibility(View.GONE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
        if (mLoginDialog != null && mLoginDialog.isVisible()) {
            mLoginDialog.dismiss();
        }
    }

    private void showAuthorizedMode() {
        mGetProfile.setVisibility(View.VISIBLE);
        mSignOut.setVisibility(View.VISIBLE);
        mClearData.setVisibility(View.VISIBLE);
        mRefreshToken.setVisibility(View.VISIBLE);
        mRevokeContainer.setVisibility(View.VISIBLE);
        mSignInBrowser.setVisibility(View.GONE);
        mSignInNative.setVisibility(View.GONE);
    }

    private void showLoggedOutMode() {
        mSignInBrowser.setVisibility(View.VISIBLE);
        mSignInNative.setVisibility(View.VISIBLE);
        mGetProfile.setVisibility(View.GONE);
        mSignOut.setVisibility(View.GONE);
        mRefreshToken.setVisibility(View.GONE);
        mClearData.setVisibility(View.GONE);
        mRevokeContainer.setVisibility(View.GONE);
        mTvStatus.setText("");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void prepareSensor() {
        if (FingerprintUtils.isSensorStateAt(FingerprintUtils.SensorState.READY, this)) {
            FingerprintManagerCompat.CryptoObject cryptoObject = mEncryptionManager.getCryptoObject();
            if (cryptoObject != null) {
                Toast.makeText(this, "use fingerprint to login", Toast.LENGTH_LONG).show();
                mFingerprintHelper = new FingerprintHelper(this);
                mFingerprintHelper.startAuth(cryptoObject);
            } else {
                Toast.makeText(this, "new fingerprint enrolled. enter pin again", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void getProfile() {
        mProgressBar.setVisibility(View.VISIBLE);
        mSessionClient.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull UserInfo result) {
                mTvStatus.setText(result.toString());
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error, AuthorizationException exception) {
                Log.d(TAG, error, exception.getCause());
                mTvStatus.setText("Error : " + exception.errorDescription);
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onLogin(String username, String password) {
        mLoginDialog.dismiss();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            mTvStatus.setText("Invalid username or password");
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        mExecutor.submit(() -> {
            try {
                mAuthenticationClient.authenticate(username, password.toCharArray(),
                        null, new AuthenticationStateHandlerAdapter() {
                            @Override
                            public void handleUnknown(
                                    AuthenticationResponse authenticationResponse) {
                                SampleActivity.this.runOnUiThread(() -> {
                                    mProgressBar.setVisibility(View.GONE);
                                    mTvStatus.setText(authenticationResponse.getStatus().name());
                                });
                            }

                            @Override
                            public void handleLockedOut(AuthenticationResponse lockedOut) {
                                SampleActivity.this.runOnUiThread(() -> {
                                    mProgressBar.setVisibility(View.GONE);
                                    mTvStatus.setText("Account locked out");
                                });
                            }

                            @Override
                            public void handleSuccess(AuthenticationResponse successResponse) {
                                String sessionToken = successResponse.getSessionToken();
                                mAuthClient.logIn(sessionToken, mPayload,
                                        new RequestCallback<AuthorizationResult,
                                                AuthorizationException>() {
                                            @Override
                                            public void onSuccess(
                                                    @NonNull AuthorizationResult result) {
                                                mTvStatus.setText("authentication authorized");
                                                showAuthorizedMode();
                                                mProgressBar.setVisibility(View.GONE);
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
                    mProgressBar.setVisibility(View.GONE);
                    mTvStatus.setText(e.getMessage());
                });
            }
        });
    }

    public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback {
        private Context mContext;
        private CancellationSignal mCancellationSignal;

        FingerprintHelper(Context context) {
            mContext = context;
        }

        void startAuth(FingerprintManagerCompat.CryptoObject cryptoObject) {
            mCancellationSignal = new CancellationSignal();
            FingerprintManagerCompat manager = FingerprintManagerCompat.from(mContext);
            manager.authenticate(cryptoObject, 0, mCancellationSignal, this, null);
        }

        void cancel() {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Toast.makeText(mContext, errString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Toast.makeText(mContext, helpString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            Cipher cipher = result.getCryptoObject().getCipher();
            Toast.makeText(mContext, "success", Toast.LENGTH_SHORT).show();
            mEncryptionManager.setCipher(cipher);
        }

        @Override
        public void onAuthenticationFailed() {
            Toast.makeText(mContext, "try again", Toast.LENGTH_SHORT).show();
        }

    }
}
