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
package com.okta.oidc.example;

import android.content.Context;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

final class Utils {
    private static final int BUFFER_SIZE = 1024;

    static String getAsset(Context context, String filename) {
        try {

            StringBuilder builder = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(
                    context.getResources().getAssets().open(filename), "UTF-8");

            char[] buffer = new char[BUFFER_SIZE];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void mockConfigurationRequest(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(get(urlMatching("/.well-known/openid-configuration"))
                .willReturn(responseDefinitionBuilder));
    }

    static void mockWebAuthorizeRequest(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(get(urlMatching("/"))
                .willReturn(responseDefinitionBuilder));
    }

    static void mockTokenRequest(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(post(urlMatching("/token"))
                .willReturn(responseDefinitionBuilder));
    }
}
