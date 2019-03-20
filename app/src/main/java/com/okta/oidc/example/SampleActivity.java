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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.okta.oidc.AuthenticateClient;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

public class SampleActivity extends AppCompatActivity {

    private static final String TAG = "SampleActivity";

    private AuthenticateClient mOktaAuth;
    private OIDCAccount mOktaAccount;
    private TextView mTvStatus;
    private Button mButton;
    private Button mSignOut;

    private Button mRevokeRefresh;
    private Button mRevokeAccess;

    private LinearLayout mRevokeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_activity_login);
        mButton = findViewById(R.id.start_button);
        mSignOut = findViewById(R.id.logout_button);
        mRevokeContainer = findViewById(R.id.revoke_token);
        mRevokeAccess = findViewById(R.id.revoke_access);
        mRevokeRefresh = findViewById(R.id.revoke_refresh);

        mRevokeRefresh.setOnClickListener(v -> mOktaAuth.revokeToken(mOktaAccount.getRefreshToken(),
                new RequestCallback<Boolean, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull Boolean result) {
                        Log.d(TAG, "Revoke refresh token : " + result);
                    }

                    @Override
                    public void onError(String error, AuthorizationException exception) {
                        Log.d(TAG, exception.error +
                                " revokeRefreshToken onError " + error, exception);
                        mTvStatus.setText(error);
                    }
                }));

        mRevokeAccess.setOnClickListener(v -> mOktaAuth.revokeToken(mOktaAccount.getAccessToken(),
                new RequestCallback<Boolean, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull Boolean result) {
                        Log.d(TAG, "Revoke Access token : " + result);
                    }

                    @Override
                    public void onError(String error, AuthorizationException exception) {
                        Log.d(TAG, exception.error +
                                " revokeAccessToken onError " + error, exception);
                        mTvStatus.setText(error);
                    }
                }));

        mSignOut.setOnClickListener(v -> {
            if (mOktaAuth.logOut(this)) {
                //already logged out
                Log.d(TAG, "Already logged out");
            }
        });

        mButton.setOnClickListener(v -> mOktaAuth.logIn(this));
        mTvStatus = findViewById(R.id.status);

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
                .withStorage(new SimpleOktaStorage(getPreferences(MODE_PRIVATE)), this)
                .withTabColor(getColorCompat(R.color.colorPrimary))
                .create();

        if (mOktaAccount.isLoggedIn()) {
            mButton.setText("Get profile");
            mButton.setOnClickListener(v -> getProfile());
            mSignOut.setVisibility(View.VISIBLE);
            mRevokeContainer.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, String.format("onActivityResult requestCode=%d" +
                "resultCode=%d PID=%d", requestCode, resultCode, android.os.Process.myPid()));
        super.onActivityResult(requestCode, resultCode, data);

        // Pass result to AuthenticateClient for processing
        boolean codeExchange = mOktaAuth.handleAuthorizationResponse(requestCode,
                resultCode, data, new ResultCallback<Boolean, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull Boolean success) {
                        Log.d(TAG, "SUCCESS");
                        if (requestCode == AuthenticateClient.REQUEST_CODE_SIGN_OUT) {
                            mTvStatus.setText("sign out success");
                            mButton.setText("Sign In");
                            mButton.setOnClickListener(v -> mOktaAuth.logIn(SampleActivity.this));
                            mSignOut.setVisibility(View.INVISIBLE);
                            mRevokeContainer.setVisibility(View.GONE);
                        } else if (requestCode == AuthenticateClient.REQUEST_CODE_SIGN_IN) {
                            mTvStatus.setText("authentication success");
                            mButton.setText("Get profile");
                            mButton.setOnClickListener(v -> getProfile());
                            mSignOut.setVisibility(View.VISIBLE);
                            mRevokeContainer.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "CANCELED!");
                        mTvStatus.setText("canceled");
                    }

                    @Override
                    public void onError(@NonNull String msg, AuthorizationException error) {
                        Log.d(TAG, error.error +
                                " onActivityResult onError " + msg, error);
                        mTvStatus.setText(msg);
                    }
                });
        if (codeExchange) {
            //TODO show loading dialog
        }
    }

    private void getProfile() {
        mOktaAuth.getUserProfile(new RequestCallback<JSONObject, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull JSONObject result) {
                mTvStatus.setText(result.toString());
            }

            @Override
            public void onError(String error, AuthorizationException exception) {
                Log.d(TAG, error, exception.getCause());
                mTvStatus.setText("Error : " + exception.errorDescription);
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
