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

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ConfigurationRequest extends
        BaseRequest<ProviderConfiguration, AuthorizationException> {
    ConfigurationRequest(HttpRequestBuilder b) {
        super();
        mRequestType = b.mRequestType;
        mUri = b.mConfig.getDiscoveryUri().buildUpon()
                .appendQueryParameter("client_id", b.mConfig.getClientId()).build();
        mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .create(b.mConn);
    }

    @Override
    public void dispatchRequest(final RequestDispatcher dispatcher,
                                final RequestCallback<ProviderConfiguration,
                                        AuthorizationException> callback) {
        dispatcher.submit(() -> {
            try {
                ProviderConfiguration config = executeRequest();
                if (config != null) {
                    dispatcher.submitResults(() -> callback.onSuccess(config));
                } else {
                    throw AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT;
                }
            } catch (AuthorizationException ae) {
                dispatcher.submitResults(() -> callback.onError(ae.error, ae));
            }
        });
    }

    @WorkerThread
    @Override
    public ProviderConfiguration executeRequest() throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection();
            JSONObject json = response.asJson();
            ProviderConfiguration configuration = new Gson()
                    .fromJson(json.toString(), ProviderConfiguration.class);
            configuration.validate();
            return configuration;
        } catch (IOException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.NETWORK_ERROR,
                    ex);
        } catch (JSONException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                    ex);
        } catch (IllegalArgumentException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT,
                    ex);
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
