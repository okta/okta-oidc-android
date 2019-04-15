package com.okta.oidc.util;


import com.okta.oidc.storage.Persistable;

public class EncryptedPersistableMock implements Persistable {
    private String mData;

    EncryptedPersistableMock(String data) {
        this.mData = data;
    }

    public String getData() {
        return mData;
    }

    @Override
    public String getKey() {
        return RESTORE.getKey();
    }

    @Override
    public String persist() {
        return this.mData;
    }

    @Override
    public boolean encrypt() {
        return RESTORE.encrypted();
    }

    public static final Restore<EncryptedPersistableMock> RESTORE = new Restore<EncryptedPersistableMock>() {
        private final String KEY = "WebRequest";

        public String getKey() {
            return KEY;
        }

        @Override
        public EncryptedPersistableMock restore(String data) {
            if(data != null) {
                return new EncryptedPersistableMock(data);
            }
            return null;
        }

        @Override
        public boolean encrypted() {
            return true;
        }
    };
}
