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

package com.okta.oidc.net.request;

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class IntrospectRequest extends
        BaseRequest<IntrospectInfo, AuthorizationException> {
    public IntrospectRequest(HttpRequestBuilder b) {
        super();
        mRequestType = b.mRequestType;
        mUri = Uri.parse(b.mProviderConfiguration.introspection_endpoint).buildUpon()
                .appendQueryParameter("client_id", b.mAccount.getClientId())
                .appendQueryParameter("token", b.mIntrospectToken)
                .appendQueryParameter("token_type_hint", b.mTokenTypeHint)
                .build();
        mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.POST)
                .create(b.mConn);
    }

    @Override
    public void dispatchRequest(RequestDispatcher dispatcher, RequestCallback<IntrospectInfo, AuthorizationException> callback) {
        dispatcher.submit(() -> {
            try {
                IntrospectInfo response = executeRequest();
                dispatcher.submitResults(() -> callback.onSuccess(response));
            } catch (AuthorizationException ae) {
                dispatcher.submitResults(() -> callback.onError(ae.error, ae));
            }
        });
    }

    @Override
    public IntrospectInfo executeRequest() throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection();
            JSONObject json = response.asJson();
            return new Gson().
                    fromJson(json.toString(), IntrospectInfo.class);
        } catch (IOException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.NETWORK_ERROR,
                    ex);
        } catch (JSONException e) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                    e);
        } finally {
            if (response != null) {
                response.disconnect();
            }
            if (exception != null) {
                throw exception;
            }
        }
        return null;
    }
}
