package com.okta.oidc.storage.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;

import static android.security.keystore.KeyProperties.BLOCK_MODE_ECB;

@TargetApi(23)
class EncryptionManagerAPI23 extends BaseEncryptionManager {
    private static final String TAG = EncryptionManagerAPI23.class.getSimpleName();
    private final boolean mIsAuthenticateUserRequired;
    private final int mUserAuthenticationValidityDurationSeconds;

    EncryptionManagerAPI23(Context context, String keyStoreName, String keyAlias, boolean isAuthenticateUserRequired, int userAuthenticationValidityDurationSeconds, boolean initCipherOnCreate) {
        super(keyStoreName, keyAlias);
        this.mKeyStoreAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
        this.mBlockMode = BLOCK_MODE_ECB;
        this.mEncryptionPadding = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
        this.mTransformationString = mKeyStoreAlgorithm + "/" + mBlockMode + "/OAEPWithSHA-256AndMGF1Padding";
        this.mIsAuthenticateUserRequired = isAuthenticateUserRequired;
        this.mUserAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
        prepare(context, initCipherOnCreate);
    }

    @Override
    boolean generateKeyPair(Context context, KeyPairGenerator generator, String keyAlias, int keySize, String encryptionPaddings, String blockMode, boolean isStrongBoxBacked, @Nullable byte[] seed) {
        try {
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(keySize)
                    .setBlockModes(blockMode)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(encryptionPaddings)
                    .setUserAuthenticationRequired(mIsAuthenticateUserRequired)
                    .setUserAuthenticationValidityDurationSeconds(mUserAuthenticationValidityDurationSeconds);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // If fingerprints list changed or will be empty
                // current keys are invalid.
                builder.setInvalidatedByBiometricEnrollment(mIsAuthenticateUserRequired);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // In Android P was introduced new way to persist keystore - StrongBox
                // If device doesn't support StrongBox, it will use TEE implementation if present.
                builder.setIsStrongBoxBacked(isStrongBoxBacked);
            }

            if (seed != null && seed.length > 0) {
                SecureRandom random = new SecureRandom(seed);
                generator.initialize(builder.build(), random);
            } else {
                generator.initialize(builder.build());
            }

            return true;
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, "initialize KeyPairGenerator: ", e);
        }
        return false;
    }

    @Override
    public boolean isHardwareBackedKeyStore() {
        boolean isHardware = false;
        try {
            if (mKeyStore != null && mKeyStore.containsAlias(mKeyAlias)) {
                PrivateKey key = (PrivateKey) mKeyStore.getKey(mKeyAlias, null);
                if (key != null) {
                    KeyFactory factory = KeyFactory.getInstance(key.getAlgorithm(), mKeyStoreName);
                    KeyInfo keyInfo;
                    try {
                        keyInfo = factory.getKeySpec(key, KeyInfo.class);
                        isHardware = keyInfo.isInsideSecureHardware();
                    } catch (InvalidKeySpecException e) {
                        Log.w(TAG, "isHardwareBackedKeyStore: ", e);
                    }
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | UnrecoverableKeyException
                | KeyStoreException e) {
            Log.w(TAG, "isHardwareBackedKeyStore: ", e);
        }
        return isHardware;
    }

    @Override
    public boolean isAuthenticateUser() {
        if (mCipher == null) {
            return false;
        }
        try {
            PrivateKey key = (PrivateKey) mKeyStore.getKey(mKeyAlias, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    KeyFactory factory = KeyFactory.getInstance(key.getAlgorithm(), mKeyStoreName);
                    KeyInfo keyInfo = factory.getKeySpec(key, KeyInfo.class);
                    if (!keyInfo.isUserAuthenticationRequired()) {
                        return true;
                    }
                } catch (NoSuchProviderException | InvalidKeySpecException error) {
                    Log.w(TAG, "Error during Read private key info: ", error);
                    return false;
                }
            }
            mCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
