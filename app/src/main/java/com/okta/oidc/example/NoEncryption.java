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

package com.okta.oidc.example;

import android.content.Context;

import com.okta.oidc.storage.security.EncryptionManager;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

/**
 * A sample on how to disable encryption. Set this implementation in
 * {@link com.okta.oidc.Okta.WebAuthBuilder#withEncryptionManager(EncryptionManager)}
 */
public class NoEncryption implements EncryptionManager {
    @Override
    public String encrypt(String value) throws GeneralSecurityException {
        return value;
    }

    @Override
    public String decrypt(String value) throws GeneralSecurityException {
        return value;
    }

    @Override
    public String getHashed(String value)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return value;
    }

    @Override
    public boolean isHardwareBackedKeyStore() {
        return true;
    }

    @Override
    public void recreateCipher() {
        //NO-OP
    }

    @Override
    public void setCipher(Cipher cipher) {
        //NO-OP
    }

    @Override
    public Cipher getCipher() {
        //NO-OP
        return null;
    }

    @Override
    public void removeKeys() {
        //NO-OP
    }

    @Override
    public void recreateKeys(Context context) {
        //NO-OP
    }

    @Override
    public boolean isUserAuthenticatedOnDevice() {
        return true;
    }

    @Override
    public boolean isValidKeys() {
        return true;
    }
}
