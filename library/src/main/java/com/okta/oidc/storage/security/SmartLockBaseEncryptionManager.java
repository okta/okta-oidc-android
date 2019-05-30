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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

@TargetApi(23)
class SmartLockBaseEncryptionManager implements EncryptionManager {
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String KEY_SIMPLE_ALIAS = "smart_simple_key_for_pin";
    private static final String KEY_AUTHORIZE_ALIAS = "smart_authorize_key_for_pin";
    private final int MIN_VALIDITY_DURATION = 5;
    private EncryptionManager mEncryptionManager;

    SmartLockBaseEncryptionManager(Context context) {
        this(context, Integer.MAX_VALUE);
    }

    SmartLockBaseEncryptionManager(Context context, int userAuthenticationValidityDurationSeconds) {
        mEncryptionManager = EncryptionManagerFactory
                .createEncryptionManager(context,
                        KEY_STORE,
                        KEY_AUTHORIZE_ALIAS,
                        true,
                        (userAuthenticationValidityDurationSeconds > MIN_VALIDITY_DURATION)
                                ? userAuthenticationValidityDurationSeconds : MIN_VALIDITY_DURATION,
                        false);
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
}
