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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.okta.oidc.storage.security.EncryptionManager;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class OktaRepository {
    private static final String TAG = OktaRepository.class.getSimpleName();

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

    public void save(Persistable persistable) throws PersistenceException {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            if(!requireHardwareBackedKeyStore
                    || (requireHardwareBackedKeyStore && encryptionManager !=null && encryptionManager.isHardwareBackedKeyStore())) {
                String encryptedData;
                try {
                    encryptedData = getEncrypted(persistable.persist());
                    storage.save(getHashed(persistable.getKey()), encryptedData);
                } catch (GeneralSecurityException | RuntimeException e) {
                    throw new PersistenceException(PersistenceException.ENCRYPT_ERROR, "Failed during encrypt data", e.getCause());
                }
            }
            if (cacheMode) {
                cacheStorage.put(getHashed(persistable.getKey()),
                        persistable.persist());
            }
        }
    }

    public <T extends Persistable> T get(Persistable.Restore<T> persistable) throws PersistenceException {
        synchronized (lock) {
            String data;
            String key = getHashed(persistable.getKey());
            if (cacheMode && cacheStorage.get(key) != null) {
                data = cacheStorage.get(key);
            } else {
                data = storage.get(key);
                try {
                    data = getDecrypted(data);
                } catch (GeneralSecurityException | RuntimeException e) {
                    String error = "Failed during decrypt data";
                    if (e instanceof RuntimeException) {
                        error += ": " + e.getMessage();
                    }
                    throw new PersistenceException(PersistenceException.DECRYPT_ERROR, error, e.getCause());
//                    storage.delete(key);
//                    data = null;
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
        return encryptionManager.encrypt(value);
    }

    private String getDecrypted(String value) throws GeneralSecurityException {
        if (encryptionManager == null) {
            return value;
        }
        return encryptionManager.decrypt(value);
    }

    private String getHashed(String value) {
        try {
            return encryptionManager.getHashed(value);
        } catch (Exception ex) {
            Log.d(TAG, "getHashed: ", ex);
            return value;
        }
    }

    public static class PersistenceException extends Exception {
        public final static int ENCRYPT_ERROR = 1;
        public final static int DECRYPT_ERROR = 2;

        private int mType;

        PersistenceException(int type, String message, Throwable cause) {
            super(message, cause);
            this.mType = type;
        }

        public int getType() {
            return mType;
        }
    }
}
