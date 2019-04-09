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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.okta.oidc.AuthenticateClient;
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

public class SampleActivity extends AppCompatActivity {

    private static final String TAG = "SampleActivity";
    @VisibleForTesting
    AuthenticateClient mOktaAuth;
    private OIDCAccount mOktaAccount;
    private TextView mTvStatus;
    private Button mSignIn;
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
        mSignIn = findViewById(R.id.sign_in);
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

        mSignOut.setOnClickListener(v -> mOktaAuth.signOutFromOkta(this));
        mClearData.setOnClickListener(v -> {
            mOktaAuth.clear();
            mTvStatus.setText("clear data");
            showLoggedOutMode();
        });

        mSignIn.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            mOktaAuth.logIn(this, mPayload);
        });


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

        if (mOktaAuth.isLoggedIn()) {
            showAuthorizedMode();
        }

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
                } else if (status == AuthorizationStatus.IN_PROGRESS) {
                    mTvStatus.setText("code exchange");
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
    protected void onStop() {
        super.onStop();
        mProgressBar.setVisibility(View.GONE);
    }

    private void showAuthorizedMode() {
        mGetProfile.setVisibility(View.VISIBLE);
        mSignOut.setVisibility(View.VISIBLE);
        mClearData.setVisibility(View.VISIBLE);
        mRefreshToken.setVisibility(View.VISIBLE);
        mRevokeContainer.setVisibility(View.VISIBLE);
        mSignIn.setVisibility(View.GONE);
    }

    private void showLoggedOutMode() {
        mSignIn.setVisibility(View.VISIBLE);
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
}
