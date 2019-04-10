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
