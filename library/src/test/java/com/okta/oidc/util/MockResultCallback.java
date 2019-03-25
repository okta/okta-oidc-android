package com.okta.oidc.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.okta.oidc.ResultCallback;

public class MockResultCallback<T, AuthorizationException extends Exception>
        implements ResultCallback<T, AuthorizationException> {

    private AuthorizationException mException;
    private T mResult;
    private String mError;

    @Override
    public void onSuccess(@NonNull T result) {
        mResult = result;
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onError(@Nullable String msg, @Nullable AuthorizationException exception) {
        mError = msg;
        mException = exception;
    }

    public T getResult() {
        return mResult;
    }

    public String getError() {
        return mError;
    }

    public AuthorizationException getException() {
        return mException;
    }
}
