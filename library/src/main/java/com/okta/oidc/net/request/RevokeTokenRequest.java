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

import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.util.AuthorizationException;

import java.io.IOException;
import java.net.HttpURLConnection;

public class RevokeTokenRequest extends BaseRequest<Boolean, AuthorizationException> {
    RevokeTokenRequest(HttpRequestBuilder b) {
        super();
        mRequestType = b.mRequestType;
        mUri = Uri.parse(b.mProviderConfiguration.revocation_endpoint).buildUpon()
                .appendQueryParameter("client_id", b.mAccount.getClientId())
                .appendQueryParameter("token", b.mTokenToRevoke)
                .build();

        mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.POST)
                .create(b.mConn);
    }

    @Override
    public void dispatchRequest(RequestDispatcher dispatcher, RequestCallback<Boolean, AuthorizationException> callback) {
        dispatcher.submit(() -> {
            try {
                Boolean success = executeRequest();
                dispatcher.submitResults(() -> callback.onSuccess(success));
            } catch (AuthorizationException ae) {
                dispatcher.submitResults(() -> callback.onError(ae.error, ae));
            }
        });
    }

    @Override
    public Boolean executeRequest() throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection();
            if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                exception = AuthorizationException.TokenRequestErrors.INVALID_CLIENT;
            } else if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (IOException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.NETWORK_ERROR,
                    ex);
        } finally {
            if (response != null) {
                response.disconnect();
            }
            if (exception != null) {
                throw exception;
            }
        }
        return false;
    }
}
