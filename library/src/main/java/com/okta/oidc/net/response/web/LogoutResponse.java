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

import android.net.Uri;

import com.google.gson.Gson;

import androidx.annotation.NonNull;

public class LogoutResponse extends WebResponse {
    private String state;

    private LogoutResponse() {
    }

    public static LogoutResponse fromUri(Uri uri) {
        LogoutResponse response = new LogoutResponse();
        response.state = uri.getQueryParameter("state");
        return response;
    }

    @Override
    public String getState() {
        return state;
    }

    @NonNull
    @Override
    public String getKey() {
        return RESTORE.getKey();
    }

    @Override
    public String persist() {
        return new Gson().toJson(this);
    }

}
