package com.okta.oidc.storage.security;

import android.content.Context;
import android.os.Build;

class EncryptionManagerFactory {
    static EncryptionManager createEncryptionManager(Context context, String keyStoreName, String keyAlias, boolean isAuthenticateUserRequired, int userAuthenticationValidityDurationSeconds, boolean initCipherOnCreate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new EncryptionManagerAPI23(context, keyStoreName, keyAlias, isAuthenticateUserRequired, userAuthenticationValidityDurationSeconds, initCipherOnCreate);
        } else {
            return new EncryptionManagerAPI18(context, keyStoreName, keyAlias, initCipherOnCreate);
        }
    }
}
