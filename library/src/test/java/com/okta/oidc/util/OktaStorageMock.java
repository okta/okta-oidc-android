package com.okta.oidc.util;

import com.okta.oidc.storage.OktaStorage;

import java.util.HashMap;
import java.util.Map;

public class OktaStorageMock implements OktaStorage {
    private Map<String, String> mInternalStorage = new HashMap<>();

    @Override
    public void save(String key, String value) {
        mInternalStorage.put(key, value);
    }

    @Override
    public String get(String key) {
        return mInternalStorage.get(key);
    }

    @Override
    public void delete(String key) {
        mInternalStorage.remove(key);
    }
}
