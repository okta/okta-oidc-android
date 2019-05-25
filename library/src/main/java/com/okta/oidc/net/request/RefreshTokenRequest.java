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

import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.util.AsciiStringListUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RefreshTokenRequest extends TokenRequest {
    RefreshTokenRequest(HttpRequestBuilder.RefreshToken b) {
        super();
        mRequestType = b.mRequestType;
        scope = b.mTokenResponse.getScope();
        mConfig = b.mConfig;
        refresh_token = b.mTokenResponse.getRefreshToken();
        mProviderConfiguration = b.mProviderConfiguration;
        mUri = Uri.parse(b.mProviderConfiguration.token_endpoint);
        client_id = b.mConfig.getClientId();
        grant_type = b.mGrantType;
        mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.POST)
                .setRequestProperty("Accept", ConnectionParameters.JSON_CONTENT_TYPE)
                .setPostParameters(buildParameters())
                .setRequestType(mRequestType)
                .create();
    }

    private Map<String, String> buildParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", client_id);
        params.put("grant_type", grant_type);
        params.put("refresh_token", refresh_token);
        params.put("scope", AsciiStringListUtil.iterableToString(Collections.singletonList(scope)));
        return params;
    }
}
