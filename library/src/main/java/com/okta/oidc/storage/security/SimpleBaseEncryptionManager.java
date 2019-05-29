package com.okta.oidc.storage.security;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

public class SimpleBaseEncryptionManager implements EncryptionManager {
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "key_for_pin";
    private final EncryptionManager encryptionManager;

    public SimpleBaseEncryptionManager(Context context) {
        encryptionManager = EncryptionManagerFactory.createEncryptionManager(context, KEY_STORE, KEY_ALIAS, false, -1, true);
    }

    @Override
    public String encrypt(String value) throws GeneralSecurityException {
        return encryptionManager.encrypt(value);
    }

    @Override
    public String decrypt(String value) throws GeneralSecurityException {
        return encryptionManager.decrypt(value);
    }

    @Override
    public String getHashed(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return encryptionManager.getHashed(value);
    }

    @Override
    public boolean isHardwareBackedKeyStore() {
        return encryptionManager.isHardwareBackedKeyStore();
    }

    @Override
    public void recreateCipher() {
        encryptionManager.recreateCipher();
    }

    @Override
    public void clearCipher() {
        encryptionManager.clearCipher();
    }

    @Override
    public boolean isAuthenticateUser() {
        return true;
    }

    @Override
    public void removeKeys() {
        encryptionManager.removeKeys();
    }
}
