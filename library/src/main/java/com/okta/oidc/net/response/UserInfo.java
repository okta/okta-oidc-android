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

package com.okta.oidc.net.response;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the user info properties of the userinfo endpoint.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#userinfo">
 * User info properties</a>
 */
public class UserInfo {
    private Map<String, Object> map;
    private JSONObject raw;

    /**
     * Instantiates a new User info with a raw JSONObject.
     *
     * @param raw the JSONObject
     */
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

    /**
     * Get the value based on the key parameter.
     *
     * @param key the key
     * @return the object
     */
    public Object get(String key) {
        return this.map.get(key);
    }

    /**
     * Gets raw JSON object.
     *
     * @return the JSONObject
     */
    public JSONObject getRaw() {
        return raw;
    }

    @NonNull
    @Override
    public String toString() {
        return this.raw.toString();
    }
}
