package com.okta.oidc.util;


import com.okta.oidc.storage.Persistable;

public class PersistableMock implements Persistable {
    private String mData;

    PersistableMock(String data) {
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

    public static final Persistable.Restore<PersistableMock> RESTORE = new Persistable.Restore<PersistableMock>() {
        private final String KEY = "WebRequest";

        public String getKey() {
            return KEY;
        }

        @Override
        public PersistableMock restore(String data) {
            if(data != null) {
                return new PersistableMock(data);
            }
            return null;
        }
    };
}
