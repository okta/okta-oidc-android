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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class WireMockStubs {
    static void mockConfigurationRequest(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(get(urlPathMatching("/.well-known/openid-configuration"))
                .willReturn(responseDefinitionBuilder));
    }

    static void mockWebAuthorizeRequest(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(get(urlPathMatching("/authorize.*"))
                .willReturn(responseDefinitionBuilder));
    }

    static void mockTokenRequest(ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(post(urlMatching("/token"))
                .willReturn(responseDefinitionBuilder));
    }
}
