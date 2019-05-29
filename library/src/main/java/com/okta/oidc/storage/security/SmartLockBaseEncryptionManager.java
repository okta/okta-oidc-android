package com.okta.oidc.storage.security;

import android.annotation.TargetApi;
import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

@TargetApi(23)
public class SmartLockBaseEncryptionManager implements EncryptionManager {
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String KEY_SIMPLE_ALIAS = "smart_simple_key_for_pin";
    private static final String KEY_AUTHORIZE_ALIAS = "smart_authorize_key_for_pin";
    private  final int MIN_VALIDITY_DURATION = 5;
    private EncryptionManager mSmartLockEncryptionManager;

    public SmartLockBaseEncryptionManager(Context context) {
        this(context, Integer.MAX_VALUE);
    }

    public SmartLockBaseEncryptionManager(Context context, int userAuthenticationValidityDurationSeconds) {
        mSmartLockEncryptionManager = EncryptionManagerFactory
                .createEncryptionManager(context,
                        KEY_STORE,
                        KEY_AUTHORIZE_ALIAS,
                        true,
                        (userAuthenticationValidityDurationSeconds > MIN_VALIDITY_DURATION) ? userAuthenticationValidityDurationSeconds : MIN_VALIDITY_DURATION,
                        false);
    }

    @Override
    public String encrypt(String value) throws GeneralSecurityException {
        return mSmartLockEncryptionManager.encrypt(value);
    }

    @Override
    public String decrypt(String value) throws GeneralSecurityException {
        return mSmartLockEncryptionManager.decrypt(value);
    }

    @Override
    public String getHashed(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return mSmartLockEncryptionManager.getHashed(value);
    }

    @Override
    public boolean isHardwareBackedKeyStore() {
        return mSmartLockEncryptionManager.isHardwareBackedKeyStore();
    }

    @Override
    public void recreateCipher() {
        mSmartLockEncryptionManager.recreateCipher();
    }

    @Override
    public void clearCipher() {
        mSmartLockEncryptionManager.clearCipher();
    }

    @Override
    public boolean isAuthenticateUser() {
        return mSmartLockEncryptionManager.isAuthenticateUser();
    }

    @Override
    public void removeKeys() {
        mSmartLockEncryptionManager.removeKeys();
    }
}
