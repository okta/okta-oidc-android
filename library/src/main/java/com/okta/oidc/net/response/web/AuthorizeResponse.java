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

//https://developer.okta.com/docs/api/resources/oidc#response-properties
public class AuthorizeResponse extends WebResponse {
    private String request_type;
    private String code;
    private String error;
    private String error_description;
    private String expires_in;
    private String id_token;
    private String scope;
    private String state;
    private String token_type;

    private AuthorizeResponse() {
        request_type = "authorize";
    }

    public static AuthorizeResponse fromUri(Uri uri) {
        AuthorizeResponse response = new AuthorizeResponse();
        response.code = uri.getQueryParameter("code");
        response.error = uri.getQueryParameter("error");
        response.error_description = uri.getQueryParameter("error_description");
        response.expires_in = uri.getQueryParameter("expires_in");
        response.id_token = uri.getQueryParameter("id_token");
        response.scope = uri.getQueryParameter("scope");
        response.state = uri.getQueryParameter("state");
        response.token_type = uri.getQueryParameter("token_type");
        return response;
    }

    @Override
    public String getState() {
        return state;
    }

    public String getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return error_description;
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

    @Override
    public boolean encrypt() {
        return RESTORE.encrypted();
    }
}
