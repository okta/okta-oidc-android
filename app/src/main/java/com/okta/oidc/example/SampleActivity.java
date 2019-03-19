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
import android.widget.TextView;

import com.okta.oidc.AuthenticateClient;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

public class SampleActivity extends AppCompatActivity {

    private static final String TAG = "SampleActivity";

    private AuthenticateClient mOktaAuth;
    private OIDCAccount mOktaAccount;
    private TextView mTvStatus;
    private Button mButton;
    private Button mSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_activity_login);
        mButton = findViewById(R.id.start_button);
        mSignOut = findViewById(R.id.logout_button);
        mSignOut.setOnClickListener(v -> {
//            if (mOktaAuth.logOut(this)) {
//                //already logged out
//                Log.d(TAG, "Already logged out");
//            }
        });

        mButton.setOnClickListener(v -> signIn());
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
                .withTabColor(getColorCompat(R.color.colorPrimary))
                .create();

        mOktaAuth.registerCallback(new ResultCallback<Boolean, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull Boolean success) {
                Log.d("SampleActivity", "SUCCESS");
                    mTvStatus.setText("authentication success");
                    mButton.setText("Get profile");
                    mButton.setOnClickListener(v -> getProfile());
                    mSignOut.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancel() {
                Log.d("SampleActivity", "CANCELED!");
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


    private void signIn() {
        mOktaAuth.logIn(this);
        //testing config change.
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void getProfile() {
        mOktaAuth.getUserProfile(new RequestCallback<JSONObject, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull JSONObject result) {
                mTvStatus.setText(result.toString());
            }

            @Override
            public void onError(String error, AuthorizationException exception) {
                Log.d(TAG, error, exception);
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
