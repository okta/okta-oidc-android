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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The interface Okta storage.
 */
public interface OktaStorage {
    /**
     * Save the data, must provide a key-value pair.
     * The data will be encrypted by the library before saving.
     *
     * @param key   the key
     * @param value the value
     */
    void save(@NonNull String key, @NonNull String value);

    /**
     * Get the value based on the key parameter.
     * The value will be decrypted by the library.
     *
     * @param key the key
     * @return the value
     */
    @Nullable
    String get(@NonNull String key);

    /**
     * Delete the data based on the key parameter.
     *
     * @param key the key
     */
    void delete(@NonNull String key);

    /**
     * Check to see if hardware backed keystore is required for storing data. If true and the device
     * doesn't have hardware support then no data will be persisted on the device.
     *
     * @return true if hardware backed keystore is required.
     */
    boolean requireHardwareBackedKeyStore();
}
