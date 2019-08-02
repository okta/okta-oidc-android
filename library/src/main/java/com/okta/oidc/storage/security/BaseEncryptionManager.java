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

package com.okta.oidc.storage.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.keystore.StrongBoxUnavailableException;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

abstract class BaseEncryptionManager implements EncryptionManager {
    private static final String TAG = BaseEncryptionManager.class.getSimpleName();
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final int MS_TO_SECOND = 1000;
    protected final String mKeyStoreName;
    protected final String mKeyAlias;
    protected boolean mIsAuthenticateUserRequired;

    protected String mKeyStoreAlgorithm;
    protected String mBlockMode;
    protected String mEncryptionPadding;
    protected String mTransformationString;

    private static final int RSA_KEY_SIZE = 2048;
    // RSA doesn't support encryption of lot amount of data.
    // Use formula to calculate the max size of chunk: (KEY_SIZE/8) - 11
    private static final int CHUNK_SIZE = (int) (((RSA_KEY_SIZE / 8) - 11) * 0.5);
    private static final String CHUNK_SEPARATOR = ",";

    protected KeyStore mKeyStore;
    protected Cipher mCipher;

    private long initCipherStart = System.currentTimeMillis();

    BaseEncryptionManager(String keyStoreName, String keyAlias) {
        this.mKeyStoreName = keyStoreName;
        this.mKeyAlias = keyAlias;
    }

    protected boolean prepare(Context context, boolean initCipher) {
        // Create KeyStore
        try {
            mKeyStore = createKeyStore();
            if (mKeyStore == null) {
                throw new RuntimeException("KeyStore is null");
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed initialize KeyStore", e.getCause());
        }

        generateKeys(context);

        // Init Cipher
        if (initCipher) {
            try {
                mCipher = createCipher(mTransformationString);
                if (mCipher == null) {
                    throw new RuntimeException("Cipher is null");
                }
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("Failed initialize Cipher", e.getCause());
            }
        }

        return true;
    }

    private void generateKeys(Context context) {
        // Check if exist instead generate new private and public keys
        try {
            if (!mKeyStore.containsAlias(mKeyAlias)) {
                KeyPairGenerator keyPairGenerator;
                try {
                    keyPairGenerator = createKeyPairGenerator();
                    if (keyPairGenerator == null) {
                        throw new RuntimeException("KeyPairGenerator is null");
                    }
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException("Failed initialize KeyPairGenerator", e.getCause());
                }
                KeyPair keyPair = null;
                try {
                    generateKeyPair(context, keyPairGenerator, mKeyAlias, RSA_KEY_SIZE,
                            mEncryptionPadding, mBlockMode, true, null);
                    keyPair = keyPairGenerator.generateKeyPair();
                } catch (ProviderException exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (exception instanceof StrongBoxUnavailableException) {
                            generateKeyPair(context, keyPairGenerator, mKeyAlias, RSA_KEY_SIZE,
                                    mEncryptionPadding, mBlockMode, false, null);
                            keyPair = keyPairGenerator.generateKeyPair();
                        }
                    } else {
                        throw new RuntimeException("Failed generate keys.", exception.getCause());
                    }
                }
                if (keyPair == null) {
                    throw new RuntimeException("Failed generate keys.");
                }
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException("Keystore exception.", e.getCause());
        }
    }

    private KeyStore createKeyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(mKeyStoreName);
        keyStore.load(null);
        return keyStore;
    }

    private KeyPairGenerator createKeyPairGenerator() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance(mKeyStoreAlgorithm, mKeyStoreName);
    }

    protected Cipher createCipher(String transformation) throws GeneralSecurityException {
        return Cipher.getInstance(transformation);
    }

    abstract boolean generateKeyPair(Context context, KeyPairGenerator generator, String keyAlias,
                                     int keySize, String encryptionPaddings, String blockMode,
                                     boolean isStrongBoxBacked, @Nullable byte[] seed);

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
                throw new RuntimeException("KeyStore exception.", e.getCause());
            }
        }
    }

    private boolean initCipher(String keyAlias, int mode) throws GeneralSecurityException {
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

    private String getUserNotAuthenticatedMessage(Cipher cipher) {
        String errorMessage = "User isn't authenticated";
        if (cipher != null) {
            errorMessage = "User was authenticated " +
                    getCipherLifeTimeSeconds() + " seconds ago";
        }
        return errorMessage;
    }

    private void initDecodeCipher(String keyAlias, int mode) throws GeneralSecurityException {
        PrivateKey key = (PrivateKey) mKeyStore.getKey(keyAlias, null);
        try {
            mCipher.init(mode, key);
        } catch (InvalidKeyException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (e instanceof UserNotAuthenticatedException) {
                    throw new OktaUserNotAuthenticateException(
                            getUserNotAuthenticatedMessage(mCipher), e);
                }
            }
            throw e;
        }
    }

    private void initEncodeCipher(String keyAlias, int mode) throws GeneralSecurityException {
        PublicKey key = mKeyStore.getCertificate(keyAlias).getPublicKey();

        // workaround for using public key
        // from https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html#known-issues
        PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm())
                .generatePublic(new X509EncodedKeySpec(key.getEncoded()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // from https://code.google.com/p/android/issues/detail?id=197719
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);

            mCipher.init(mode, unrestricted, spec);
        } else {
            mCipher.init(mode, unrestricted);
        }
    }

    @Override
    public String encrypt(String inputString) throws GeneralSecurityException {
        if (inputString != null && inputString.length() > 0) {
            if (mCipher == null) {
                throw new InvalidParameterException(
                        "Cipher is null. Please initialize proper cipher");
            }
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
        try {
            if (encryptedString != null && encryptedString.length() > 0) {
                if (mCipher == null) {
                    throw new InvalidParameterException(
                            "Cipher is null. Please initialize proper cipher");
                }
                if (initCipher(mKeyAlias, Cipher.DECRYPT_MODE)) {
                    StringBuilder decryptedBuilder = new StringBuilder();
                    String[] chunks = encryptedString.split(CHUNK_SEPARATOR);
                    for (String chunk : chunks) {
                        byte[] bytes = Base64.decode(chunk, Base64.NO_WRAP);
                        decryptedBuilder.append(new String(mCipher.doFinal(bytes)));
                    }
                    return decryptedBuilder.toString();
                }
            }
            return encryptedString;
        } catch (IllegalBlockSizeException e) {
            // We generate keys using UserAuthenticationValidityDurationSeconds parameter.
            // We decrypt data by chunk. This exception could be if this validity duration ended
            // during decryption. In this reason we check cause exception and provide valid
            // exception to user space
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && e.getCause() instanceof UserNotAuthenticatedException) {
                throw new OktaUserNotAuthenticateException(
                        getUserNotAuthenticatedMessage(mCipher), e);
            }
            throw e;
        }
    }

    @Override
    public String getHashed(String value) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] result = digest.digest(value.getBytes(DEFAULT_CHARSET));
        return toHex(result);
    }

    @Override
    public abstract boolean isHardwareBackedKeyStore();

    @Override
    public void recreateCipher() {
        try {
            mCipher = createCipher(mTransformationString);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed init Cipher", e.getCause());
        }
        resetTimer();
    }

    @Override
    public void removeKeys() {
        deleteInvalidKey(mKeyAlias);
    }

    @Override
    public void recreateKeys(Context context) {
        prepare(context, false);
    }

    @Override
    public void setCipher(Cipher cipher) {
        mCipher = cipher;
    }

    @Override
    public Cipher getCipher() {
        return mCipher;
    }

    private void resetTimer() {
        initCipherStart = System.currentTimeMillis();
    }

    private long getCipherLifeTimeSeconds() {
        return (System.currentTimeMillis() - initCipherStart) / MS_TO_SECOND;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static class OktaUserNotAuthenticateException extends UserNotAuthenticatedException {
        OktaUserNotAuthenticateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
