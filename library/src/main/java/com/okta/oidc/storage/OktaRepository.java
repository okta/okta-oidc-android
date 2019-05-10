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
import com.okta.oidc.storage.security.SimpleEncryptionManager;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class OktaRepository {
    private static final String TAG = OktaRepository.class.getSimpleName();

    private final OktaStorage storage;
    private final EncryptionManager encryptionManager;
    final Map<String, String> cacheStorage = new HashMap<>();

    private final Object lock = new Object();

    public OktaRepository(@Nullable OktaStorage storage, Context context,
                          @Nullable EncryptionManager encryptionManager) {
        if (storage == null) {
            this.storage = new SimpleOktaStorage(context);
        } else {
            this.storage = storage;
        }
        if (encryptionManager != null) {
            this.encryptionManager = encryptionManager;
        } else {
            this.encryptionManager = buildSimpleEncryptionManager(context);
        }
    }

    public void save(Persistable persistable) {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            storage.save(getHashed(persistable.getKey()),
                    getEncrypted(persistable.persist()));
            cacheStorage.put(getHashed(persistable.getKey()),
                    getEncrypted(persistable.persist()));
        }
    }

    public <T extends Persistable> T get(Persistable.Restore<T> persistable) {
        synchronized (lock) {
            String data = null;
            String key = getHashed(persistable.getKey());
            if (cacheStorage.get(key) != null) {
                data = cacheStorage.get(key);
            } else {
                data = storage.get(key);
            }
            data = getDecrypted(data);
            return persistable.restore(data);
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

    private String getEncrypted(String value) {
        if (encryptionManager == null) {
            return value;
        }
        try {
            return encryptionManager.encrypt(value);
        } catch (GeneralSecurityException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        } catch (IOException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        }
    }

    private String getDecrypted(String value) {
        if (encryptionManager == null) {
            return value;
        }
        try {
            return encryptionManager.decrypt(value);
        } catch (GeneralSecurityException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        } catch (IOException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        }
    }

    String getHashed(String value) {
        try {
            return encryptionManager.getHashed(value);
        } catch (NoSuchAlgorithmException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        } catch (UnsupportedEncodingException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        }
    }

    private EncryptionManager buildSimpleEncryptionManager(Context context) {
        try {
            return new SimpleEncryptionManager(context);
        } catch (IOException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return null;
        } catch (GeneralSecurityException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return null;
        }
    }
}
