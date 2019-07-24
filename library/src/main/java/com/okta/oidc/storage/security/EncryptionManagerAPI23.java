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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.Nullable;

import java.security.GeneralSecurityException;
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

@TargetApi(Build.VERSION_CODES.M)
class EncryptionManagerAPI23 extends BaseEncryptionManager {
    private static final String TAG = EncryptionManagerAPI23.class.getSimpleName();

    private final int mValidityDurationSeconds;

    EncryptionManagerAPI23(Context context, String keyStoreName, String keyAlias,
                           boolean isAuthenticateUserRequired,
                           int userAuthenticationValidityDurationSeconds,
                           boolean initCipherOnCreate) {
        super(keyStoreName, keyAlias);
        this.mKeyStoreAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
        this.mBlockMode = BLOCK_MODE_ECB;
        this.mEncryptionPadding = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
        this.mTransformationString = mKeyStoreAlgorithm + "/" + mBlockMode
                + "/OAEPWithSHA-256AndMGF1Padding";
        this.mIsAuthenticateUserRequired = isAuthenticateUserRequired;
        this.mValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
        prepare(context, initCipherOnCreate);
    }

    @Override
    boolean generateKeyPair(Context context, KeyPairGenerator generator, String keyAlias,
                            int keySize, String encryptionPadding, String blockMode,
                            boolean isStrongBoxBacked, @Nullable byte[] seed) {
        try {
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(keySize)
                    .setBlockModes(blockMode)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(encryptionPadding)
                    .setUserAuthenticationRequired(mIsAuthenticateUserRequired)
                    .setUserAuthenticationValidityDurationSeconds(mValidityDurationSeconds);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // If fingerprints list changed or will be empty
                // current keys are invalid.
                // This option doesn't work, because we set
                // UserAuthenticationValidityDurationSeconds parameter and this option works only
                // if this parameter set to not positive
                // builder.setInvalidatedByBiometricEnrollment(mIsAuthenticateUserRequired);
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
    public boolean isUserAuthenticatedOnDevice() {
        if (mCipher == null) {
            return false;
        }
        try {
            PrivateKey key = (PrivateKey) mKeyStore.getKey(mKeyAlias, null);
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
            mCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (GeneralSecurityException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isValidKeys() {
        try {
            Cipher cipher = createCipher(mTransformationString);
            PrivateKey key = (PrivateKey) mKeyStore.getKey(mKeyAlias, null);
            if (key == null) {
                return false;
            }
            try {
                KeyFactory factory = KeyFactory.getInstance(key.getAlgorithm(), mKeyStoreName);
                KeyInfo keyInfo = factory.getKeySpec(key, KeyInfo.class);
            } catch (NoSuchProviderException | InvalidKeySpecException error) {
                Log.w(TAG, "Error during Read private key info: ", error);
                return false;
            }
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (GeneralSecurityException e) {
            return false;
        }

        return true;
    }
}
