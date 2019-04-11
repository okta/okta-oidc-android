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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class LoginDialog extends DialogFragment {
    private EditText mPassword;
    private EditText mUsername;
    private Button mSignIn;

    private LoginDialogListener mListener;

    public LoginDialog() {
        //NO-OP
    }

    public interface LoginDialogListener {
        void onLogin(String username, String password);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login_dialog, container, false);

        mPassword = v.findViewById(R.id.password);
        mUsername = v.findViewById(R.id.username);
        mSignIn = v.findViewById(R.id.sign_in);
        mSignIn.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onLogin(mUsername.getText().toString(), mPassword.getText().toString());
            }
        });
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListener = null;
    }

    public void setListener(LoginDialogListener listener) {
        mListener = listener;
    }
}