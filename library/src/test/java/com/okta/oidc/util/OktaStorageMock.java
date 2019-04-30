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
