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
import android.support.annotation.NonNull;

import com.google.gson.Gson;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthorizeResponse response = (AuthorizeResponse) o;

        if (request_type != null ? !request_type.equals(response.request_type) : response.request_type != null)
            return false;
        if (code != null ? !code.equals(response.code) : response.code != null) return false;
        if (error != null ? !error.equals(response.error) : response.error != null) return false;
        if (error_description != null ? !error_description.equals(response.error_description) : response.error_description != null)
            return false;
        if (expires_in != null ? !expires_in.equals(response.expires_in) : response.expires_in != null)
            return false;
        if (id_token != null ? !id_token.equals(response.id_token) : response.id_token != null)
            return false;
        if (scope != null ? !scope.equals(response.scope) : response.scope != null) return false;
        if (state != null ? !state.equals(response.state) : response.state != null) return false;
        return token_type != null ? token_type.equals(response.token_type) : response.token_type == null;
    }
}
