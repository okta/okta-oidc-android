package com.okta.oidc.net.response;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UserInfo {
    private Map<String, Object> map;
    private JSONObject raw;

    public UserInfo(JSONObject raw) {
        this.raw = raw;
        if (raw == null) {
            this.map = new HashMap<>();
        } else {
            this.map = new Gson().fromJson(
                    raw.toString(), new TypeToken<HashMap<String, Object>>() {
                    }.getType()
            );
        }
    }

    public Object get(String key) {
        return this.map.get(key);
    }

    public JSONObject getRaw() {
        return raw;
    }

    @NonNull
    @Override
    public String toString() {
        return this.raw.toString();
    }
}
