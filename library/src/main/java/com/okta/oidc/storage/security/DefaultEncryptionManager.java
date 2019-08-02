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
 * A implementation of {@link EncryptionManager}.
 */
public class DefaultEncryptionManager implements EncryptionManager {
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "key_for_pin";
    private final EncryptionManager mEncryptionManager;

    /**
     * Constructor requires a context.
     *
     * @param context context
     */
    public DefaultEncryptionManager(Context context) {
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
    public void setCipher(Cipher cipher) {
        mEncryptionManager.setCipher(cipher);
    }

    @Override
    public Cipher getCipher() {
        return mEncryptionManager.getCipher();
    }

    @Override
    public boolean isUserAuthenticatedOnDevice() {
        return true;
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
