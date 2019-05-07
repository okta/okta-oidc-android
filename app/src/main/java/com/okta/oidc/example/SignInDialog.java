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

/**
 * Login Dialog for username and password entry.
 * For the Authentication API to get a sessionToken.
 */
public class SignInDialog extends DialogFragment {
    private EditText mPassword;
    private EditText mUsername;

    private SignInDialogListener mListener;

    /**
     * Instantiates a new Login dialog.
     */
    public SignInDialog() {
        //NO-OP
    }

    /**
     * The interface dialog listener.
     */
    public interface SignInDialogListener {
        /**
         * On SignIn.
         *
         * @param username the username
         * @param password the password
         */
        void onSignIn(String username, String password);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.signin_dialog, container, false);

        mPassword = view.findViewById(R.id.password);
        mUsername = view.findViewById(R.id.username);
        Button signIn = view.findViewById(R.id.submit);
        signIn.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSignIn(mUsername.getText().toString(), mPassword.getText().toString());
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListener = null;
    }

    /**
     * Sets listener.
     *
     * @param listener the listener
     */
    public void setListener(SignInDialogListener listener) {
        mListener = listener;
    }
}
