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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

/**
 * Encryption Manager is responsible for encrypting and decrypting all data
 * that is going to be stored in {@link com.okta.oidc.storage.OktaStorage}.
 */
public interface EncryptionManager {

    /**
     * encrypts value.
     *
     * @param value value as a string.
     * @return encrypted value.
     * @throws GeneralSecurityException if has problems with algorithms used.
     * @throws IOException              if failed to open input stream
     */
    String encrypt(String value) throws GeneralSecurityException;

    /**
     * decrypts encrypted value.
     *
     * @param value encrypted value as a string.
     * @return decrypted value.
     * @throws GeneralSecurityException if has problems with algorithms used.
     * @throws IOException              if failed to open input stream
     */
    String decrypt(String value) throws GeneralSecurityException;

    /**
     * generates SHA-2 hash.
     *
     * @param value string to generate hash from.
     * @return hashed value.
     * @throws NoSuchAlgorithmException     if device does not support SHA-2.
     * @throws UnsupportedEncodingException if wrong encoding used.
     */
    String getHashed(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException;

    /**
     * if the key store is backed by hardware.
     *
     * @return true if hardware backed keystore is supported
     */
    boolean isHardwareBackedKeyStore();

    /**
     * recreate cipher using internal settings
     */
    void recreateCipher();

    /**
     * remove current cipher
     */
    void clearCipher();

    /**
     * remove current keys
     */
    void removeKeys();

    /**
     * if user authenticated and cipher is valid to use private key.
     *
     * @return true if hardware backed keystore is supported
     */
    boolean isAuthenticateUser();
}
