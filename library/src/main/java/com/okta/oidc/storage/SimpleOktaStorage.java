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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import static android.content.Context.MODE_PRIVATE;

/**
 * A implementation of {@link OktaStorage}.
 * The implementation uses SharedPreferences in private mode to save data.
 * The data will be encrypted by the library before saving.
 */
@SuppressLint("ApplySharedPref")
public class SimpleOktaStorage implements OktaStorage {
    @VisibleForTesting
    protected SharedPreferences prefs;

    /**
     * Instantiates a new instance.
     * Uses default class name as preferences file.
     *
     * @param context the context
     */
    public SimpleOktaStorage(Context context) {
        this(context, null);
    }

    /**
     * Instantiates a new instance.
     *
     * @param context  the context
     * @param prefName the preferences file name.
     */
    public SimpleOktaStorage(Context context, String prefName) {
        prefs = context.getSharedPreferences(prefName == null ?
                SimpleOktaStorage.class.getCanonicalName() : prefName, MODE_PRIVATE);
    }

    @Override
    public void save(@NonNull String key, @NonNull String value) {
        prefs.edit().putString(key, value).commit();
    }

    @Nullable
    @Override
    public String get(@NonNull String key) {
        return prefs.getString(key, null);
    }


    @Override
    public void delete(@NonNull String key) {
        prefs.edit().remove(key).commit();
    }
}
