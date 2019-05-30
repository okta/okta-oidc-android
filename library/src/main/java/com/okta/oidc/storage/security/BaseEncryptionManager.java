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
import java.security.UnrecoverableKeyException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

@TargetApi(8)
abstract class BaseEncryptionManager implements EncryptionManager {
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
    protected Cipher mDecryptCipher;
    private Cipher mEncryptCipher;
    private PrivateKey mPrivateKey;
    private boolean mInitCipher;

    private long initCipherStart = System.currentTimeMillis();

    public BaseEncryptionManager(String keyStoreName, String keyAlias) {
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

        // Check if exist instead generate new private and public keys
        try {
            if (!mKeyStore.containsAlias(mKeyAlias)) {
                KeyPairGenerator mKeyPairGenerator;
                try {
                    mKeyPairGenerator = createKeyPairGenerator();
                    if (mKeyPairGenerator == null) {
                        throw new RuntimeException("KeyPairGenerator is null");
                    }
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException("Failed initialize KeyPairGenerator", e.getCause());
                }
                KeyPair keyPair = null;
                try {
                    generateKeyPair(context, mKeyPairGenerator, mKeyAlias, RSA_KEY_SIZE,
                            mEncryptionPadding, mBlockMode, true, null);
                    keyPair = mKeyPairGenerator.generateKeyPair();
                } catch (ProviderException exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (exception instanceof StrongBoxUnavailableException) {
                            generateKeyPair(context, mKeyPairGenerator, mKeyAlias, RSA_KEY_SIZE,
                                    mEncryptionPadding, mBlockMode, false, null);
                            keyPair = mKeyPairGenerator.generateKeyPair();
                        }
                    } else {
                        throw new RuntimeException("Failed generate keys.", exception.getCause());
                    }
                }
                if (keyPair == null) {
                    throw new RuntimeException("Failed generate keys.");
                }
            }
            mPrivateKey = (PrivateKey) mKeyStore.getKey(mKeyAlias, null);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Keystore exception.", e.getCause());
        }
        return true;
    }

    private KeyStore createKeyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(mKeyStoreName);
        keyStore.load(null);
        return keyStore;
    }

    private KeyPairGenerator createKeyPairGenerator() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance(mKeyStoreAlgorithm, mKeyStoreName);
    }

    private Cipher createCipher(String transformation) throws GeneralSecurityException {
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

    private void initDecodeCipher(String keyAlias, int mode) throws GeneralSecurityException {
        if (mDecryptCipher == null || mInitCipher) {
            //don't recreate the cipher just initialize it.
            if (mDecryptCipher == null) {
                mDecryptCipher = createCipher(mTransformationString);
            }
            try {
                mDecryptCipher.init(mode, mPrivateKey);
            } catch (InvalidKeyException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (e instanceof UserNotAuthenticatedException) {
                        mInitCipher = true;
                        String errorMessage = "User wasn't authenticated";
                        throw new OktaUserNotAuthenticateException(errorMessage, e);

                    }
                }
                throw e;
            }
        }
    }

    private void initEncodeCipher(String keyAlias, int mode) throws GeneralSecurityException {
        PublicKey key = mKeyStore.getCertificate(keyAlias).getPublicKey();

        // workaround for using public key
        // from https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html#known-issues
        PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm())
                .generatePublic(new X509EncodedKeySpec(key.getEncoded()));
        if (mEncryptCipher == null) {
            mEncryptCipher = createCipher(mTransformationString);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // from https://code.google.com/p/android/issues/detail?id=197719
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1,
                    PSource.PSpecified.DEFAULT);

            mEncryptCipher.init(mode, unrestricted, spec);
        } else {
            mEncryptCipher.init(mode, unrestricted);
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
                    byte[] bytes = mEncryptCipher.doFinal(chunk);
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
                    decryptedBuilder.append(new String(mDecryptCipher.doFinal(bytes)));
                }
                return decryptedBuilder.toString();
            }
        }
        return encryptedString;
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
    public Cipher getCipher() {
        return mDecryptCipher;
    }

    @Override
    public void setCipher(Cipher cipher) {
        mDecryptCipher = cipher;
    }

    @Override
    public void recreateCipher() {
        try {
            mEncryptCipher = createCipher(mTransformationString);
            mDecryptCipher = createCipher(mTransformationString);
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
    public void clearCipher() {
        mEncryptCipher = null;
        mDecryptCipher = null;
    }

    private void resetTimer() {
        initCipherStart = System.currentTimeMillis();
    }

    private long getCipherLifeTime() {
        return System.currentTimeMillis() - initCipherStart;
    }

    @TargetApi(23)
    public static class OktaUserNotAuthenticateException extends UserNotAuthenticatedException {
        OktaUserNotAuthenticateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
