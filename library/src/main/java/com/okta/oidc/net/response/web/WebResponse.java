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
package com.okta.oidc.net.response.web;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.LogoutRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.storage.Persistable;

public abstract class WebResponse implements Persistable {

    public abstract String getState();

    public static final Persistable.Restore<WebResponse> RESTORE_ME = new Persistable.Restore<WebResponse>() {
        private final String KEY = "WebResponse";

        @NonNull
        @Override
        public String getKey() {
            return KEY;
        }

        @Override
        public WebResponse restore(@Nullable String data) {
            if (data != null) {
                if (data.startsWith("authorize")) {
                    return new Gson().fromJson(data, AuthorizeResponse.class);
                } else {
                    return new Gson().fromJson(data, LogoutResponse.class);
                }
            }
            return null;
        }

        @Override
        public boolean encrypted() {
            return false;
        }
    };
}
