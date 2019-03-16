package com.okta.oidc.storage;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.TokenResponse;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

public class OktaRepository {
    private static final String TAG = OktaRepository.class.getSimpleName();

    private final OktaStorage storage;
    private final EncryptionManager encryptionManager;

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
            } else {
                storage.save(persistable.getKey(), persistable.persist());
            }
        }
    }

    public Object restore(Persistable.Restore persistable) {
        synchronized (lock) {
            String data = null;
            if (persistable.encrypted()) {
                data = getDecrypted(storage.get(getHashed(persistable.getKey())));
            } else {
                data = storage.get(persistable.getKey());
            }
            return persistable.restore(data);
        }
    }

    public void delete(Persistable persistable) {
        if (persistable == null) {
            return;
        }
        synchronized (lock) {
            if (persistable.encrypt()) {
                storage.delete(getHashed(persistable.getKey()));
            } else {
                storage.delete(persistable.getKey());
            }
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