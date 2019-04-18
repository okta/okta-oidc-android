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
import com.okta.oidc.AuthenticateClient;
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.Okta;
import com.okta.oidc.OktaGeneric;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.factory.client.async.AsyncAuthClientFactory;
import com.okta.oidc.factory.client.async.BrowserAsyncAuthClient;
import com.okta.oidc.factory.client.async.NativeAsyncAuthClient;
import com.okta.oidc.factory.client.sync.BrowserSyncAuthClient;
import com.okta.oidc.factory.client.sync.NativeSyncAuthClient;
import com.okta.oidc.factory.client.sync.SyncAuthClientFactory;
import com.okta.oidc.factory.session.SessionClientFactory;
import com.okta.oidc.factory.session.async.AsyncSessionClient;
import com.okta.oidc.factory.session.async.AsyncSessionClientFactory;
import com.okta.oidc.factory.session.sync.SyncSessionClient;
import com.okta.oidc.factory.session.sync.SyncSessionClientFactory;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectResponse;
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
    @VisibleForTesting
    AuthenticateClient mOktaAuth;
    @VisibleForTesting
    OIDCAccount mOktaAccount;
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
            mOktaAuth.introspectToken(
                    mOktaAuth.getTokens().getRefreshToken(), TokenTypeHint.REFRESH_TOKEN,
                    new RequestCallback<IntrospectResponse, AuthorizationException>() {
                        @Override
                        public void onSuccess(@NonNull IntrospectResponse result) {
                            mTvStatus.setText("RefreshToken active: " + result.active);
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
            mOktaAuth.introspectToken(
                    mOktaAuth.getTokens().getAccessToken(), TokenTypeHint.ACCESS_TOKEN,
                    new RequestCallback<IntrospectResponse, AuthorizationException>() {
                        @Override
                        public void onSuccess(@NonNull IntrospectResponse result) {
                            mTvStatus.setText("AccessToken active: " + result.active);
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
            mOktaAuth.introspectToken(
                    mOktaAuth.getTokens().getIdToken(), TokenTypeHint.ID_TOKEN,
                    new RequestCallback<IntrospectResponse, AuthorizationException>() {
                        @Override
                        public void onSuccess(@NonNull IntrospectResponse result) {
                            mTvStatus.setText("IdToken active: " + result.active);
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
            mOktaAuth.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
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
            Tokens tokens = mOktaAuth.getTokens();
            if (tokens != null && tokens.getRefreshToken() != null) {
                mProgressBar.setVisibility(View.VISIBLE);
                mOktaAuth.revokeToken(mOktaAuth.getTokens().getRefreshToken(),
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
            Tokens tokens = mOktaAuth.getTokens();
            if (tokens != null && tokens.getAccessToken() != null) {
                mProgressBar.setVisibility(View.VISIBLE);
                mOktaAuth.revokeToken(mOktaAuth.getTokens().getAccessToken(),
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
            mOktaAuth.signOutFromOkta(this);
        });
        mClearData.setOnClickListener(v -> {
            mOktaAuth.clear();
            mTvStatus.setText("clear data");
            showLoggedOutMode();
        });

        mSignInBrowser.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            mOktaAuth.logIn(this, mPayload);
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
        mOktaAccount = new OIDCAccount.Builder()
                .clientId("0oajqehiy6p81NVzA0h7")
                .redirectUri("com.oktapreview.samples-test:/callback")
                .endSessionRedirectUri("com.oktapreview.samples-test:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://samples-test.oktapreview.com")
                .create();

        mOktaAuth = new AuthenticateClient.Builder()
                .withAccount(mOktaAccount)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .withTabColor(getColorCompat(R.color.colorPrimary))
                .create();

        Okta okta = new Okta.Builder()
                .withAccount(mOktaAccount)
                .withStorage(new SimpleOktaStorage(this))
                .withAuthenticationClientFactory(new SyncAuthClientFactory())
                .withSessionClientFactory(new SyncSessionClientFactory())
                .create();

        // Good
        BrowserSyncAuthClient browserAuthorization = okta.getBrowserAuthorizationClient();
        NativeSyncAuthClient nativeAuthorization = okta.getNativeAuthorizationClient();
        SyncSessionClient SessionClient = okta.getSessionClient();
        // Bad
        BrowserAsyncAuthClient browserAsyncAuthorization = okta.getBrowserAuthorizationClient();
        BrowserAsyncAuthClient browserNativeAsyncAuthorization = okta.getNativeAuthorizationClient();
        SyncSessionClient sessionClient = okta.getNativeAuthorizationClient();

        Okta okta = new Okta.Builder()
                .withAccount(mOktaAccount)
                .withStorage(new SimpleOktaStorage(this))
                .withAuthenticationClientFactory(new AsyncAuthClientFactory())
                .withSessionClientFactory(new AsyncSessionClientFactory())
                .create();

        //Good
        BrowserAsyncAuthClient asyncBrowserAuthorization = okta.getBrowserAuthorizationClient(BrowserAsyncAuthClient.class);
        NativeAsyncAuthClient asyncNativeAuthorization = okta.getNativeAuthorizationClient(NativeAsyncAuthClient.class);
        AsyncSessionClient asyncSession = okta.getSessionClient(AsyncSessionClient.class);
        // Compile time error
        NativeSyncAuthClient syncBrowserAuthorization = okta.getBrowserAuthorizationClient(NativeSyncAuthClient.class);
        // Compile time error
        AsyncSessionClient asyncSession2 = okta.getBrowserAuthorizationClient(AsyncSessionClient.class);

        //Bad: runtime error
        NativeSyncAuthClient syncBrowserAuthorization2 = okta.getNativeAuthorizationClient(NativeSyncAuthClient.class);
        SyncSessionClient syncSession3 = okta.getSessionClient(SyncSessionClient.class);


        // Bad: ugly creating interface.
        OktaGeneric<BrowserSyncAuthClient, NativeSyncAuthClient, SyncSessionClient> oktaGeneric = new OktaGeneric.Builder<BrowserSyncAuthClient, NativeSyncAuthClient, SyncSessionClient>()
                .withAccount(mOktaAccount)
                .withStorage(new SimpleOktaStorage(this))
                .withAuthenticationClientFactory(new SyncAuthClientFactory())
                .withSessionClientFactory(new SyncSessionClientFactory())
                .create();

        //GOOD
        BrowserSyncAuthClient browserSyncAuthClient = oktaGeneric.getBrowserAuthorizationClient();
        NativeSyncAuthClient nativeSyncAuthorizationClient = oktaGeneric.getNativeAuthorizationClient();
        SyncSessionClient sessionClient2 = oktaGeneric.getSessionClient();
        //GOOD: compile time error
        BrowserAsyncAuthClient browserSyncAuthClient2 = oktaGeneric.getBrowserAuthorizationClient();
        NativeAsyncAuthClient nativeSyncAuthorizationClient2 = oktaGeneric.getNativeAuthorizationClient();
        AsyncSessionClient asyncSessionClient2 = oktaGeneric.getSessionClient();

        if (mOktaAuth.isLoggedIn()) {
            showAuthorizedMode();
        }

        setupCallback();
    }

    @VisibleForTesting
    void setupCallback() {
        mOktaAuth.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
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
        if (mOktaAuth.getAuthorizationStatus() == AuthorizationStatus.IN_PROGRESS) {
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
        mOktaAuth.getUserProfile(new RequestCallback<JSONObject, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull JSONObject result) {
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
                        mOktaAuth.logIn(sessionToken, mPayload);
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
