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

import androidx.annotation.RestrictTo;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.request.web.AuthorizeRequest.Parameters;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.util.AuthorizationException;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class NativeAuthorizeRequest extends BaseRequest<AuthorizeResponse, AuthorizationException> {
    private Parameters mParameters;

    public NativeAuthorizeRequest(Parameters parameters, HttpConnectionFactory conn) {
        mParameters = parameters;
        mUri = mParameters.toUri();
        mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .create(conn);
    }

    public Parameters getParameters() {
        return mParameters;
    }

    @Override
    public void dispatchRequest(RequestDispatcher dispatcher,
                                RequestCallback<AuthorizeResponse,
                                        AuthorizationException> callback) {
        dispatcher.submit(() -> {
            try {
                AuthorizeResponse response = executeRequest();
                dispatcher.submitResults(() -> callback.onSuccess(response));
            } catch (AuthorizationException ae) {
                dispatcher.submitResults(() -> callback.onError(ae.error, ae));
            }
        });
    }

    @Override
    public AuthorizeResponse executeRequest() throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection();
            if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                exception = AuthorizationException.TokenRequestErrors.INVALID_CLIENT;
            } else if (response.getStatusCode() == HttpURLConnection.HTTP_OK
                    || response.getStatusCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                Uri locationUri = Uri.parse(response.getHeaderField("Location"));
                return AuthorizeResponse.fromUri(locationUri);
            }
        } catch (IOException ex) {
            exception = new AuthorizationException(ex.getMessage(), ex);
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
