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

package com.okta.oidc.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.okta.oidc.storage.OktaStorage;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * A sample on how to replace the default SharedPreferenceStorage with the encrypted version
 * from the androidx library. If the storage is already encrypting the data, make sure to disable
 * encryption by providing a empty encryption manager like {@link NoEncryption} and set this storage
 * in {@link com.okta.oidc.Okta.WebAuthBuilder#withStorage(OktaStorage)}
 */
public class EncryptedSharedPreferenceStorage implements OktaStorage {
    private SharedPreferences prefs;

    /**
     * Instantiates a new instance.
     *
     * @param context the context
     * @throws GeneralSecurityException thrown if unable to instantiate due to encryption errors
     * @throws IOException              throw by EncryptedSharedPreferences
     */
    public EncryptedSharedPreferenceStorage(Context context)
            throws GeneralSecurityException, IOException {
        this(context, null);
    }

    /**
     * Instantiates a new instance.
     *
     * @param context  the context
     * @param prefName the preferences file name.
     * @throws GeneralSecurityException thrown if unable to instantiate due to encryption errors
     * @throws IOException              throw by EncryptedSharedPreferences
     */
    public EncryptedSharedPreferenceStorage(Context context, String prefName)
            throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        prefs = EncryptedSharedPreferences.create(TextUtils.isEmpty(prefName) ?
                        EncryptedSharedPreferenceStorage.class.getCanonicalName() : prefName,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void save(@NonNull String key, @NonNull String value) {
        prefs.edit().putString(key, value).commit();
    }

    @Nullable
    @Override
    public String get(@NonNull String key) {
        return prefs.getString(key, null);
    }


    @SuppressLint("ApplySharedPref")
    @Override
    public void delete(@NonNull String key) {
        prefs.edit().remove(key).commit();
    }
}
