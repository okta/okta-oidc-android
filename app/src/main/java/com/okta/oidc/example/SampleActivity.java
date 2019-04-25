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

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.okta.oidc.clients.AsyncNativeAuth;
import com.okta.oidc.clients.AsyncNativeAuthClientFactory;
import com.okta.oidc.clients.AsyncWebAuth;
import com.okta.oidc.clients.AsyncWebAuthClientFactory;
import com.okta.oidc.clients.SyncNativeAuth;
import com.okta.oidc.clients.SyncWebAuth;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SampleActivity extends AppCompatActivity implements LoginDialog.LoginDialogListener {

    private static final String TAG = "SampleActivity";
    AsyncWebAuth asyncWebAuthClient;
    AsyncNativeAuth asyncNativeAuthClient;
    AsyncSession asyncSessionClient;

    @VisibleForTesting
    OIDCConfig mOktaAccount;
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
    private static final String FIRE_FOX = "org.mozilla.firefox";

    private LinearLayout mRevokeContainer;

    @VisibleForTesting
    AuthenticationPayload mPayload;

    protected AuthenticationClient mAuthenticationClient;
    private LoginDialog mLoginDialog;
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
/*
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
*/
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
            asyncSessionClient.introspectToken(
                    asyncSessionClient.getTokens().getRefreshToken(), TokenTypeHint.REFRESH_TOKEN,
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
            asyncSessionClient.introspectToken(
                    asyncSessionClient.getTokens().getAccessToken(), TokenTypeHint.ACCESS_TOKEN,
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
            asyncSessionClient.introspectToken(
                    asyncSessionClient.getTokens().getIdToken(), TokenTypeHint.ID_TOKEN,
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
            asyncSessionClient.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
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
            Tokens tokens = asyncSessionClient.getTokens();
            if (tokens != null && tokens.getRefreshToken() != null) {
                mProgressBar.setVisibility(View.VISIBLE);
                asyncSessionClient.revokeToken(asyncSessionClient.getTokens().getRefreshToken(),
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
            Tokens tokens = asyncSessionClient.getTokens();
            if (tokens != null && tokens.getAccessToken() != null) {
                mProgressBar.setVisibility(View.VISIBLE);
                asyncSessionClient.revokeToken(asyncSessionClient.getTokens().getAccessToken(),
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
            asyncWebAuthClient.signOutFromOkta(this);
        });
        mClearData.setOnClickListener(v -> {
            asyncSessionClient.clear();
            mTvStatus.setText("clear data");
            showLoggedOutMode();
        });

        mSignInBrowser.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            asyncWebAuthClient.logIn(this, mPayload);
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
        //samples sdk test
        mOktaAccount = new OIDCConfig.Builder()
                .clientId("0oajqehiy6p81NVzA0h7")
                .redirectUri("com.oktapreview.samples-test:/callback")
                .endSessionRedirectUri("com.oktapreview.samples-test:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://samples-test.oktapreview.com")
                .create();

        AsyncWebAuth mWebOktaAuth = new Okta.AsyncWebBuilder()
                .withConfig(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withCallbackExecutor(null)
                .withTabColor(0)
                .supportedBrowsers(null)
                .create();

        AsyncNativeAuth mNativeOktaAuth = new Okta.AsyncNativeBuilder()
                .withConfig(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withCallbackExecutor(null)
                .create();

        SyncWebAuth mWebSyncOktaAuth = new Okta.SyncWebBuilder()
                .withConfig(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withTabColor(0)
                .supportedBrowsers(null)
                .create();

        SyncNativeAuth mNativeSyncOktaAuth = new Okta.SyncNativeBuilder()
                .withConfig(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .create();


        AsyncWebAuth mWebOktaAuthPro =  new Okta.Builder<AsyncWebAuth>()
                .withConfig(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withAuthenticationClientFactory(new AsyncWebAuthClientFactory(null, Color.BLUE, null))
                .create();

        AsyncNativeAuth mNativeOktaAuthPro = new Okta.Builder<AsyncNativeAuth>()
                .withConfig(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withAuthenticationClientFactory(new AsyncNativeAuthClientFactory(null))
                .create();




        //GOOD
        asyncWebAuthClient = mWebOktaAuthPro;
        asyncSessionClient = mWebOktaAuthPro.getSessionClient();
        asyncNativeAuthClient = mNativeOktaAuthPro;

        if (asyncSessionClient.isLoggedIn()) {
            showAuthorizedMode();
        }

        setupCallback();
    }

    @VisibleForTesting
    void setupCallback() {
        asyncWebAuthClient.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
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
            public void onError(@NonNull String msg, AuthorizationException error) {
                Log.d("SampleActivity", error.error +
                        " onActivityResult onError " + msg, error);
                mTvStatus.setText(msg);
            }
        }, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (asyncWebAuthClient.isInProgress()) {
            mProgressBar.setVisibility(View.VISIBLE);
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

    private void getProfile() {
        mProgressBar.setVisibility(View.VISIBLE);
        asyncSessionClient.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
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

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    private int getColorCompat(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(color);
        } else {
            return getResources().getColor(color);
        }
    }

    private void hideKeyBoard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
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
                mAuthenticationClient.authenticate(username, password.toCharArray(), null, new AuthenticationStateHandlerAdapter() {
                    @Override
                    public void handleUnknown(AuthenticationResponse authenticationResponse) {
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
                        //TODO: Implement callback here
                        asyncNativeAuthClient.logIn(sessionToken, mPayload, new RequestCallback<AuthorizationResult, AuthorizationException>() {
                            @Override
                            public void onSuccess(@NonNull AuthorizationResult result) {
                                mTvStatus.setText("authentication authorized");
                                showAuthorizedMode();
                                mProgressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(String error, AuthorizationException exception) {
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
}
