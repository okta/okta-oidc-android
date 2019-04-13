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
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

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

    public static String getJwt(String issuer, String nonce, Date expiredDate, Date issuedAt,
                                String... audience) {
        JwtBuilder builder = Jwts.builder();
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        Map<String, Object> map = new HashMap<>();
        map.put(Claims.AUDIENCE, Arrays.asList(audience));

        return builder
                .addClaims(map)
                .claim("nonce", nonce)
                .setIssuer(issuer)
                .setSubject("sub")
                .setExpiration(expiredDate)
                .setIssuedAt(issuedAt)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
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
