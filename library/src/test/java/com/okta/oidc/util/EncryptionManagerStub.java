package com.okta.oidc.util;

import android.content.Context;

import com.okta.oidc.storage.security.EncryptionManager;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

public class EncryptionManagerStub implements EncryptionManager {

    public static final String STUPID_SALT = "stupidSalt";
    private static final String DEFAULT_CHARSET = "UTF-8";

    private boolean mHardwareBacked;

    public EncryptionManagerStub() {
        mHardwareBacked = true;
    }

    public EncryptionManagerStub(boolean hardwareBacked) {
        mHardwareBacked = hardwareBacked;
    }

    @Override
    public String encrypt(String value) throws GeneralSecurityException {
        if (value != null && value.length() > 0) {
            return value + STUPID_SALT;
        }
        return null;
    }

    @Override
    public String decrypt(String value) throws GeneralSecurityException {
        if (value != null && value.length() > 0) {
            return value.replace(STUPID_SALT, "");
        } else {
            return null;
        }
    }

    @Override
    public String getHashed(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] result = digest.digest(value.getBytes(DEFAULT_CHARSET));

        return toHex(result);
    }

    @Override
    public boolean isHardwareBackedKeyStore() {
        return mHardwareBacked;
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    @Override
    public void recreateCipher() {

    }

    @Override
    public void setCipher(Cipher cipher) {

    }

    @Override
    public Cipher getCipher() {
        return null;
    }

    @Override
    public void removeKeys() {

    }

    @Override
    public void recreateKeys(Context context) {

    }

    @Override
    public boolean isUserAuthenticatedOnDevice() {
        return true;
    }

    @Override
    public boolean isValidKeys() {
        return true;
    }
}
