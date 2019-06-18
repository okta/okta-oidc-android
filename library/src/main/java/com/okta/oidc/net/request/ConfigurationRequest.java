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
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ConfigurationRequest extends
        BaseRequest<ProviderConfiguration, AuthorizationException> {
    private boolean mIsOAuth2;

    ConfigurationRequest(HttpRequestBuilder.Configuration b) {
        super();
        mRequestType = b.mRequestType;
        mIsOAuth2 = b.mConfig.isOAuth2Configuration();
        mUri = b.mConfig.getDiscoveryUri().buildUpon()
                .appendQueryParameter("client_id", b.mConfig.getClientId()).build();

        mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.GET)
                .setRequestType(mRequestType)
                .create();
    }

    @WorkerThread
    @Override
    public ProviderConfiguration executeRequest(OktaHttpClient client)
            throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection(client);

            JSONObject json = response.asJson();
            ProviderConfiguration configuration = new Gson()
                    .fromJson(json.toString(), ProviderConfiguration.class);
            configuration.validate(mIsOAuth2);
            return configuration;
        } catch (IOException ex) {
            exception = new AuthorizationException(ex.getMessage(), ex);
        } catch (JSONException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR,
                    ex);
        } catch (IllegalArgumentException ex) {
            exception = AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT,
                    ex);
        } catch (Exception e) {
            exception = new AuthorizationException(e.getMessage(), e);
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
