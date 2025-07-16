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

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;

import androidx.annotation.Nullable;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

class EncryptionManagerAPI18 extends BaseEncryptionManager {
    private static final String TAG = EncryptionManagerAPI18.class.getSimpleName();
    private static final int RSA_CALENDAR_MAX_YEARS = 100;
    private static final int RSA_CALENDAR_HOURS_OFFSET = -26;

    EncryptionManagerAPI18(Context context, String keyStoreName, String keyAlias,
                           boolean initCipherOnCreate, boolean isAuthenticateUserRequired) {
        super(keyStoreName, keyAlias);
        this.mKeyStoreAlgorithm = "RSA";
        this.mBlockMode = "ECB";
        this.mEncryptionPadding = "PKCS1Padding";
        this.mTransformationString = mKeyStoreAlgorithm + "/" + mBlockMode + "/"
                + mEncryptionPadding;
        this.mIsAuthenticateUserRequired = isAuthenticateUserRequired;

        prepare(context, initCipherOnCreate);
    }

    @Override
    boolean generateKeyPair(Context context, KeyPairGenerator generator, String keyAlias,
                            int keySize, String encryptionPadding, String blockMode,
                            boolean isStrongBoxBacked, @Nullable byte[] seed) {
        Calendar startDate = Calendar.getInstance();
        //probable fix for the timezone issue
        startDate.add(Calendar.HOUR_OF_DAY, RSA_CALENDAR_HOURS_OFFSET);
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.YEAR, RSA_CALENDAR_MAX_YEARS);

        try {
            KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(context)
                    .setAlias(keyAlias)
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(new X500Principal(
                            "CN = Secured Preference Store, O = Devliving Online"))
                    .setStartDate(startDate.getTime())
                    .setEndDate(endDate.getTime());
            builder.setKeySize(keySize);
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
        return false;
    }

    @Override
    public boolean isUserAuthenticatedOnDevice() {
        return true;
    }

    @Override
    public boolean isValidKeys() {
        try {
            PrivateKey key = (PrivateKey) mKeyStore.getKey(mKeyAlias, null);
            return key != null;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
