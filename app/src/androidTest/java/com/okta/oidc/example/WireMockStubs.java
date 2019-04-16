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
