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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

/**
 * Encryption manager that does not require authentication to use.
 */
public class SimpleBaseEncryptionManager implements EncryptionManager {
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "key_for_pin";
    private final EncryptionManager mEncryptionManager;

    /**
     * Instantiates a new encryption manager.
     *
     * @param context the context
     */
    public SimpleBaseEncryptionManager(Context context) {
        mEncryptionManager = EncryptionManagerFactory.createEncryptionManager(context, KEY_STORE,
                KEY_ALIAS, false, -1, true);
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
    public void clearCipher() {
        mEncryptionManager.clearCipher();
    }

    @Override
    public boolean isUserAuthenticatedOnDevice() {
        return true;
    }

    @Override
    public Cipher getCipher() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCipher(Cipher cipher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeKeys() {
        mEncryptionManager.removeKeys();
    }
}
