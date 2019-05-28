package com.okta.oidc.storage.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

@TargetApi(8)
public abstract class BaseEncryptionManager implements EncryptionManager {
    private static final String TAG = BaseEncryptionManager.class.getSimpleName();
    private static final String DEFAULT_CHARSET = "UTF-8";

    protected final String mKeyStoreName;
    protected final String mKeyAlias;

    protected String mKeyStoreAlgorithm;
    protected String mBlockMode;
    protected String mEncryptionPadding;
    protected String mTransformationString;

    private static final int RSA_KEY_SIZE = 2048;
    // RSA doesn't support encryption of lot amount of data.
    // Use formula to calculate the max size of chunk: (KEY_SIZE/8) - 11
    //TODO: need to understand why formula need extra multiple 0.5
    private static final int CHUNK_SIZE = (int) (((RSA_KEY_SIZE / 8) - 11) * 0.5);
    private static final String CHUNK_SEPARATOR = ",";

    protected KeyStore mKeyStore;
    protected Cipher mCipher;

    private long initCipherStartStart = System.currentTimeMillis();

    public BaseEncryptionManager(String keyStoreName, String keyAlias) {
        this.mKeyStoreName = keyStoreName;
        this.mKeyAlias = keyAlias;
    }

    protected boolean prepare(Context context, boolean initCipher) {
        // Create KeyStore
        mKeyStore = createKeyStore();
        if (mKeyStore == null) {
            return false;
        }

        KeyPairGenerator mKeyPairGenerator = createKeyPairGenerator();
        if (mKeyPairGenerator == null) {
            return false;
        }

        // Check if exist instead generate new private and public keys
        try {
            if (!mKeyStore.containsAlias(mKeyAlias)) {
                try {
                    generateKeyPair(context, mKeyPairGenerator, mKeyAlias, RSA_KEY_SIZE, mEncryptionPadding, mBlockMode, true, null);
                    mKeyPairGenerator.generateKeyPair();
                } catch (Exception exception) {
                    generateKeyPair(context, mKeyPairGenerator, mKeyAlias, RSA_KEY_SIZE, mEncryptionPadding, mBlockMode, false, null);
                    mKeyPairGenerator.generateKeyPair();
                }
            }
        } catch (KeyStoreException e) {
            Log.d(TAG, "keyStore: ", e);
            return false;
        }


        // Init Cipher
        if(initCipher) {
            mCipher = createCipher(mTransformationString);
            if (mCipher == null) {
                return false;
            }
        }

        return true;
    }

    private KeyStore createKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(mKeyStoreName);
            keyStore.load(null);
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException
                | CertificateException e) {
            Log.d(TAG, "getKeyStore: ", e);
        }
        return null;
    }

    private KeyPairGenerator createKeyPairGenerator() {
        try {
            return KeyPairGenerator
                    .getInstance(mKeyStoreAlgorithm, mKeyStoreName);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            Log.e(TAG, "getKeyPairGenerator: ", e);
        }
        return null;
    }

    protected Cipher createCipher(String transformation) {
        try {
            return Cipher.getInstance(transformation);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "getCipher: ", e);
        }
        return null;
    }

    abstract boolean generateKeyPair(Context context, KeyPairGenerator generator, String keyAlias, int keySize, String encryptionPaddings, String blockMode, boolean isStrongBoxBacked, @Nullable byte[] seed);

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    private void deleteInvalidKey(String keyAlias) {
        if (mKeyStore != null) {
            try {
                mKeyStore.deleteEntry(keyAlias);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean initCipher(String keyAlias, int mode) throws KeyStoreException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException,
            InvalidKeyException, UnrecoverableKeyException {
            switch (mode) {
                case Cipher.ENCRYPT_MODE:
                    initEncodeCipher(keyAlias, mode);
                    break;

                case Cipher.DECRYPT_MODE:
                    initDecodeCipher(keyAlias, mode);
                    break;
                default:
                    return false; //this cipher is only for encode\decode
            }
            return true;
    }

    private void initDecodeCipher(String keyAlias, int mode) throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, InvalidKeyException {
        PrivateKey key = (PrivateKey) mKeyStore.getKey(keyAlias, null);
        try {
            mCipher.init(mode, key);
        } catch (Exception e) {
            String errorMessage = "User wasn't authenticated";
            if(mCipher != null) {
                errorMessage = "User was authenticated "+getCipherLifeTime()/1000+" seconds ago";
            }
            throw new RuntimeException(errorMessage, e.getCause());
        }
    }

    private void initEncodeCipher(String keyAlias, int mode) throws KeyStoreException, InvalidKeySpecException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
            PublicKey key = mKeyStore.getCertificate(keyAlias).getPublicKey();

            // workaround for using public key
            // from https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html#known-issues
            PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm())
                    .generatePublic(new X509EncodedKeySpec(key.getEncoded()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // from https://code.google.com/p/android/issues/detail?id=197719
                OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1,
                        PSource.PSpecified.DEFAULT);

                mCipher.init(mode, unrestricted, spec);
            } else {
                mCipher.init(mode, unrestricted);
            }
    }

    @Override
    public String encrypt(String inputString) throws GeneralSecurityException {
        if (inputString != null && inputString.length() > 0) {
            if (initCipher(mKeyAlias, Cipher.ENCRYPT_MODE)) {
                StringBuilder encryptedBuilder = new StringBuilder();
                int chunkStart = 0;
                while (chunkStart < inputString.length()) {
                    int chunkEnd = chunkStart + CHUNK_SIZE;
                    byte[] chunk = inputString.substring(
                            chunkStart,
                            chunkEnd < inputString.length() ? chunkEnd : inputString.length()
                    ).getBytes();
                    byte[] bytes = mCipher.doFinal(chunk);
                    chunkStart = chunkEnd;
                    encryptedBuilder.append(Base64.encodeToString(bytes, Base64.NO_WRAP));
                    if (chunkStart < inputString.length()) {
                        encryptedBuilder.append(CHUNK_SEPARATOR);
                    }
                }
                return encryptedBuilder.toString();
            }
        }
        return inputString;
    }

    @Override
    public String decrypt(String encryptedString) throws GeneralSecurityException {
        if (encryptedString != null && encryptedString.length() > 0) {
            if (initCipher(mKeyAlias, Cipher.DECRYPT_MODE)) {
                StringBuilder decryptedBuilder = new StringBuilder();
                String[] chunks = encryptedString.split(CHUNK_SEPARATOR);
                for (String chunk : chunks) {
                    byte[] bytes = Base64.decode(chunk, Base64.NO_WRAP);
                    try {
                        decryptedBuilder.append(new String(mCipher.doFinal(bytes)));
                    } catch (Exception e) {
                        throw e;
                    }
                }
                return decryptedBuilder.toString();
            }
        }
        return encryptedString;
    }

    @Override
    public String getHashed(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] result = digest.digest(value.getBytes(DEFAULT_CHARSET));
        return toHex(result);
    }

    @Override
    public abstract boolean isHardwareBackedKeyStore();

    @Override
    public void recreateCipher() {
        mCipher = createCipher(mTransformationString);
        resetTimer();
    }

    @Override
    public void clearCipher() {
        mCipher = null;
    }

    private void resetTimer() {
        initCipherStartStart = System.currentTimeMillis();
    }

    private long getCipherLifeTime() {
        return System.currentTimeMillis() - initCipherStartStart;
    }

}
