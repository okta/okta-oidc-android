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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

/**
 * A implementation of {@link EncryptionManager} which requires user authentication when
 * using keys by OS. The private keys are locked in the key store.
 */
@TargetApi(Build.VERSION_CODES.M)
public class GuardedEncryptionManager implements EncryptionManager {
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String KEY_AUTHORIZE_ALIAS = "smart_authorize_key_for_pin";
    private static final int MIN_VALIDITY_DURATION = 10;
    private EncryptionManager mEncryptionManager;

    /**
     * Constructor requires a context. It create encryption manager which requires device
     * authorization only once.
     *
     * @param context context
     */
    public GuardedEncryptionManager(Context context) {
        this(context, Integer.MAX_VALUE);
    }

    /**
     * Constructor requires a context and validity duration time for keys in seconds.
     * If user authorized to use keys by OS, these keys are available without authorization
     * for "validity duration time" value. If this time expired, user need to authorize again.
     * The minimum value is 10. It's not possible to set value < 10.
     *
     * @param context                                   context
     * @param userAuthenticationValidityDurationSeconds validity duration time in seconds
     */
    public GuardedEncryptionManager(Context context,
                                    int userAuthenticationValidityDurationSeconds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mEncryptionManager = EncryptionManagerFactory
                    .createEncryptionManager(context,
                            KEY_STORE,
                            KEY_AUTHORIZE_ALIAS,
                            true,
                            (userAuthenticationValidityDurationSeconds > MIN_VALIDITY_DURATION)
                                    ? userAuthenticationValidityDurationSeconds
                                    : MIN_VALIDITY_DURATION,
                            false);
        } else {
            throw new IllegalStateException("This class supports API23+");
        }
    }

    @Override
    public String encrypt(String value) throws GeneralSecurityException {
        return mEncryptionManager.encrypt(value);
    }

    @Override
    public String decrypt(String value) throws GeneralSecurityException {
        return mEncryptionManager.decrypt(value);
    }

    @Override
    public String getHashed(String value) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        return mEncryptionManager.getHashed(value);
    }

    @Override
    public boolean isHardwareBackedKeyStore() {
        return mEncryptionManager.isHardwareBackedKeyStore();
    }

    @Override
    public void recreateCipher() {
        mEncryptionManager.recreateCipher();
    }

    @Override
    public void setCipher(Cipher cipher) {
        mEncryptionManager.setCipher(cipher);
    }

    @Override
    public Cipher getCipher() {
        return mEncryptionManager.getCipher();
    }

    @Override
    public boolean isUserAuthenticatedOnDevice() {
        return mEncryptionManager.isUserAuthenticatedOnDevice();
    }

    @Override
    public void removeKeys() {
        mEncryptionManager.removeKeys();
    }

    @Override
    public void recreateKeys(Context context) {
        mEncryptionManager.recreateKeys(context);
    }

    @Override
    public boolean isValidKeys() {
        return mEncryptionManager.isValidKeys();
    }
}
