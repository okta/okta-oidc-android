package com.okta.oidc.storage;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class OktaRepository {
    private static final String TAG = OktaRepository.class.getSimpleName();

    private final OktaStorage storage;
    private final EncryptionManager encryptionManager;
    private final Map<String, String> cacheStorage = new HashMap<>();

    private final Object lock = new Object();

    public OktaRepository(OktaStorage storage, Context context) {
        this.storage = storage;
        this.encryptionManager = buildEncryptionManager(context);
    }

    public void save(Persistable persistable) {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            if (persistable.encrypt()) {
                storage.save(getHashed(persistable.getKey()),
                        getEncrypted(persistable.persist()));
                cacheStorage.put(getHashed(persistable.getKey()),
                        getEncrypted(persistable.persist()));
            } else {
                storage.save(persistable.getKey(), persistable.persist());
                cacheStorage.put(persistable.getKey(), persistable.persist());
            }
        }
    }

    public <T extends Persistable> T get(Persistable.Restore<T> persistable) {
        synchronized (lock) {
            String data = null;
            String key = (persistable.encrypted()) ?
                    getHashed(persistable.getKey()) : persistable.getKey();
            if(cacheStorage.get(key) != null) {
                data = cacheStorage.get(key);
            } else {
                data = storage.get(key);
            }

            if(persistable.encrypted()) {
                data = getDecrypted(data);
            }

            return persistable.restore(data);
        }
    }

    public void delete(Persistable persistable) {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            String key = (persistable.encrypt()) ?
                    getHashed(persistable.getKey()) : persistable.getKey();
            storage.delete(key);
            cacheStorage.remove(key);
        }
    }

    private String getEncrypted(String value) {
        if (encryptionManager == null) return value;
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
        if (encryptionManager == null) return value;
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

    private String getHashed(String value) {
        try {
            return EncryptionManager.getHashed(value);
        } catch (NoSuchAlgorithmException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        } catch (UnsupportedEncodingException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return value;
        }
    }

    private EncryptionManager buildEncryptionManager(Context context) {
        try {
            return new EncryptionManager(context,
                    context.getSharedPreferences("Encryption", Context.MODE_PRIVATE));
        } catch (IOException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return null;
        } catch (GeneralSecurityException ex) {
            Log.d(TAG, "getEncrypted: " + ex.getCause());
            return null;
        }
    }
}