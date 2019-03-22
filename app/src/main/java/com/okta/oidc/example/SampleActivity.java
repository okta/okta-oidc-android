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
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.okta.oidc.AuthenticateClient;
import com.okta.oidc.AuthorisationStatus;
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

        mSignOut.setOnClickListener(v -> mOktaAuth.logOut(this));

        mButton.setOnClickListener(v -> mOktaAuth.logIn(this));
        mTvStatus = findViewById(R.id.status);

        //samples sdk test
        mOktaAccount = new OIDCAccount.Builder()
                .clientId("0oahnzhsegzYjqETc0h7")
                .redirectUri("com.lohika.android.test:/callback")
                .endSessionRedirectUri("com.lohika.android.test:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://lohika-um.oktapreview.com")
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


        mOktaAuth.registerCallback(new ResultCallback<AuthorisationStatus, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull AuthorisationStatus status) {
                Log.d("SampleActivity", "AUTHORIZED");
                if (status == AuthorisationStatus.AUTHORIZED) {
                    mTvStatus.setText("authentication authorized");
                    mButton.setText("Get profile");
                    mButton.setOnClickListener(v -> getProfile());
                    mSignOut.setVisibility(View.VISIBLE);
                    mRevokeContainer.setVisibility(View.VISIBLE);
                } else if (status == AuthorisationStatus.LOGGED_OUT) {
                    mTvStatus.setText("log out su");
                    mButton.setText("Log in");
                    mButton.setOnClickListener(v -> mOktaAuth.logIn(SampleActivity.this));
                    mSignOut.setVisibility(View.GONE);
                    mRevokeContainer.setVisibility(View.GONE);
                    mTvStatus.setText("");
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
