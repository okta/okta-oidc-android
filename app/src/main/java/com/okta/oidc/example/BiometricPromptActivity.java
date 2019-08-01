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

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.AuthenticationCallback;
import androidx.biometric.BiometricPrompt.AuthenticationResult;
import androidx.biometric.BiometricPrompt.PromptInfo;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executors;

/**
 * Classes that extends Activity will need to delegate to a extension of FragmentActivity
 * to show a backwards compatible biometric prompt. androidx.biometrics.BiometricsPrompt
 * requires FragmentActivity to inject a fragment that shows a prompt similar to the latest android
 * version.
 */
public class BiometricPromptActivity extends FragmentActivity {
    private BiometricPrompt mPrompt;
    private AuthenticationCallback mCallback = new AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                mPrompt.cancelAuthentication();
            }
            BiometricPromptActivity.this.setResult(RESULT_CANCELED);
            finish();
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            BiometricPromptActivity.this.setResult(RESULT_OK);
            finish();
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            BiometricPromptActivity.this.setResult(RESULT_CANCELED);
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        mPrompt = new BiometricPrompt(this,
                Executors.newSingleThreadExecutor(), mCallback);

        PromptInfo info = new PromptInfo.Builder()
                .setTitle("Confirm credentials")
                .setNegativeButtonText("Cancel")
                .build();
        mPrompt.authenticate(info);
    }
}
