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

package com.okta.oidc.storage;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.okta.oidc.storage.security.BaseEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;

import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.ProviderException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.IllegalBlockSizeException;

import static com.okta.oidc.storage.OktaRepository.EncryptionException.DECRYPT_ERROR;
import static com.okta.oidc.storage.OktaRepository.EncryptionException.ENCRYPT_ERROR;
import static com.okta.oidc.storage.OktaRepository.EncryptionException.HARDWARE_BACKED_ERROR;
import static com.okta.oidc.storage.OktaRepository.EncryptionException.ILLEGAL_BLOCK_SIZE;
import static com.okta.oidc.storage.OktaRepository.EncryptionException.INVALID_KEYS_ERROR;
import static com.okta.oidc.storage.OktaRepository.EncryptionException.KEYGUARD_AUTHENTICATION_ERROR;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class OktaRepository {
    private static final String TAG = OktaRepository.class.getSimpleName();
    private static final int MAX_WAIT_TIME_MILLISECONDS_BEFORE_RETRY = 100;

    private final OktaStorage storage;
    private EncryptionManager encryptionManager;
    private boolean requireHardwareBackedKeyStore;
    private boolean cacheMode;
    final Map<String, String> cacheStorage = new HashMap<>();

    private final Object lock = new Object();

    public OktaRepository(OktaStorage storage, Context context,
                          @Nullable EncryptionManager encryptionManager,
                          boolean requireHardwareBackedKeyStore,
                          boolean cacheMode) {
        this.storage = storage;
        this.cacheMode = cacheMode;
        this.requireHardwareBackedKeyStore = requireHardwareBackedKeyStore;
        this.encryptionManager = encryptionManager;
    }

    public void setEncryptionManager(EncryptionManager encryptionManager) {
        this.encryptionManager = encryptionManager;
    }

    public void save(Persistable persistable) throws EncryptionException {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            if (!requireHardwareBackedKeyStore || encryptionManager != null &&
                    encryptionManager.isHardwareBackedKeyStore()) {
                String encryptedData;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        encryptedData = getEncrypted(persistable.persist());
                        storage.save(getHashed(persistable.getKey()), encryptedData);
                    } catch (BaseEncryptionManager.OktaUserNotAuthenticateException e) {
                        String error = "Failed during encrypt data: " + e.getMessage();
                        throw new EncryptionException(ENCRYPT_ERROR, error, e.getCause());
                    } catch (IllegalBlockSizeException e) {
                        String error = "Unable to encrypt " + persistable.getKey() + " the " +
                                "cipher algorithm may not be supported on this device" +
                                e.getMessage();
                        throw new EncryptionException(ILLEGAL_BLOCK_SIZE, error,
                                e.getCause());
                    } catch (GeneralSecurityException e) {
                        throw new EncryptionException(INVALID_KEYS_ERROR, e.getMessage(),
                                e.getCause());
                    } catch (InvalidParameterException e) {
                        throw new EncryptionException(ENCRYPT_ERROR, e.getMessage(),
                                e.getCause());
                    }
                } else {
                    try {
                        encryptedData = getEncrypted(persistable.persist());
                        storage.save(getHashed(persistable.getKey()), encryptedData);
                    } catch (GeneralSecurityException e) {
                        throw new EncryptionException(INVALID_KEYS_ERROR, e.getMessage(),
                                e.getCause());
                    }
                }
            } else {
                throw new EncryptionException(HARDWARE_BACKED_ERROR,
                        "Client require hardware backed keystore, " +
                                "but EncryptionManager doesn't support it.", null);
            }
            if (cacheMode) {
                cacheStorage.put(getHashed(persistable.getKey()),
                        persistable.persist());
            }
        }
    }

    public <T extends Persistable> T get(Persistable.Restore<T> persistable)
            throws EncryptionException {
        synchronized (lock) {
            String data;
            String key = getHashed(persistable.getKey());
            if (cacheMode && cacheStorage.get(key) != null) {
                data = cacheStorage.get(key);
            } else {
                data = storage.get(key);

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        data = getDecrypted(data);
                    } catch (BaseEncryptionManager.OktaUserNotAuthenticateException e) {
                        String error = "User not authenticated and try to decrypt data: " +
                                e.getMessage();
                        throw new EncryptionException(KEYGUARD_AUTHENTICATION_ERROR, error,
                                e.getCause());
                    } catch (IllegalBlockSizeException e) {
                        String error = "Unable to decrypt " + persistable.getKey() + " the key " +
                                "used may be invalidated. Please clear data and try again. " +
                                e.getMessage();
                        throw new EncryptionException(ILLEGAL_BLOCK_SIZE, error,
                                e.getCause());
                    } catch (GeneralSecurityException e) {
                        throw new EncryptionException(INVALID_KEYS_ERROR, e.getMessage(),
                                e.getCause());
                    } catch (InvalidParameterException e) {
                        throw new EncryptionException(DECRYPT_ERROR, e.getMessage(),
                                e.getCause());
                    }
                } else {
                    try {
                        data = getDecrypted(data);
                    } catch (GeneralSecurityException e) {
                        throw new EncryptionException(INVALID_KEYS_ERROR, e.getMessage(),
                                e.getCause());
                    }
                }

                if (cacheMode) {
                    cacheStorage.put(key, data);
                }
            }
            return persistable.restore(data);
        }
    }

    public boolean contains(Persistable.Restore persistable) {
        synchronized (lock) {
            String key = getHashed(persistable.getKey());
            return (cacheMode && cacheStorage.get(key) != null) || storage.get(key) != null;
        }
    }

    public void delete(Persistable persistable) {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            String key = getHashed(persistable.getKey());
            storage.delete(key);
            cacheStorage.remove(key);
        }
    }

    public void delete(String key) {
        if (key == null) {
            return;
        }
        synchronized (lock) {
            String hashedKey = getHashed(key);
            storage.delete(hashedKey);
            cacheStorage.remove(hashedKey);
        }
    }

    private String getEncrypted(String value) throws GeneralSecurityException {
        if (encryptionManager == null) {
            return value;
        }
        try {
            return encryptionManager.encrypt(value);
        } catch (ProviderException | GeneralSecurityException ex) {
            sleep();
            return encryptionManager.encrypt(value);
        }
    }

    private String getDecrypted(String value) throws GeneralSecurityException {
        if (encryptionManager == null) {
            return value;
        }
        try {
            return encryptionManager.decrypt(value);
        } catch (ProviderException | GeneralSecurityException ex) {
            sleep();
            return encryptionManager.decrypt(value);
        }
    }

    private String getHashed(String value) {
        try {
            return encryptionManager.getHashed(value);
        } catch (Exception ex) {
            Log.d(TAG, "getHashed: ", ex);
            return value;
        }
    }

    // Copyright 2017 Google Inc.
    // https://github.com/google/tink/blob/cb814f1e1b69caf6211046bee083a730625a3cf9/java_src/src/main/java/com/google/crypto/tink/integration/android/AndroidKeystoreAesGcm.java
    private static void sleep() {
        int waitTimeMillis = (int) (Math.random() * MAX_WAIT_TIME_MILLISECONDS_BEFORE_RETRY);
        try {
            Thread.sleep(waitTimeMillis);
        } catch (InterruptedException ex) {
            // Ignored.
        }
    }

    public static class EncryptionException extends Exception {
        public static final int ENCRYPT_ERROR = 1;
        public static final int HARDWARE_BACKED_ERROR = 3;
        public static final int INVALID_KEYS_ERROR = 4;
        public static final int KEYGUARD_AUTHENTICATION_ERROR = 5;
        public static final int DECRYPT_ERROR = 6;
        public static final int ILLEGAL_BLOCK_SIZE = 7;

        private int mType;

        EncryptionException(int type, String message, Throwable cause) {
            super(message, cause);
            this.mType = type;
        }

        public int getType() {
            return mType;
        }
    }
}
