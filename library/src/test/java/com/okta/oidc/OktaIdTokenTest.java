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
package com.okta.oidc;

import android.net.Uri;

import com.okta.oidc.net.request.HttpRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.DateUtil;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.KeyPair;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import static com.okta.oidc.util.TestValues.getAuthorizeRequest;
import static com.okta.oidc.util.TestValues.getAuthorizeResponse;
import static org.junit.Assert.*;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)

public class OktaIdTokenTest {
    private OIDCAccount mAccount;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        mAccount = TestValues.getAccountWithUrl("com.okta.test");
    }

    @Test
    public void validate() throws AuthorizationException {
        OktaIdToken token = OktaIdToken.parseIdToken(JsonStrings.VALID_ID_TOKEN);
        assertEquals("RS256", token.mHeader.alg);

        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);

        String jws = Jwts.builder()
                .setIssuer(mAccount.getDiscoveryUri().toString())
                .setSubject("sub")
                .setAudience(mAccount.getClientId())
                .setExpiration(DateUtil.getTomorrow())
                .setNotBefore(DateUtil.getNow())
                .setIssuedAt(DateUtil.getNow())
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);

        //TODO generate correct nonce and code.
        TokenRequest tokenRequest =
                getTokenRequest(getAuthorizeRequest(mAccount, "nonce", "state"),
                        getAuthorizeResponse("state", "code"));
        idToken.validate(tokenRequest, System::currentTimeMillis);
        assertNotNull(idToken);
        assertNotNull(idToken.mHeader);
        assertNotNull(idToken.mClaims);
    }

    @Test
    public void parseValidIdToken() {
        OktaIdToken token = OktaIdToken.parseIdToken(JsonStrings.VALID_ID_TOKEN);
        assertNotNull(token.mClaims);
        assertNotNull(token.mSignature);
        assertNotNull(token.mHeader);
        assertEquals("RS256", token.mHeader.alg);
    }

    @Test
    public void parseInValidIdToken() {
        mExpectedEx.expect(IllegalArgumentException.class);
        OktaIdToken token = OktaIdToken.parseIdToken(JsonStrings.INVALID_ID_TOKEN);
        assertNull(token);
    }

    private TokenRequest getTokenRequest(AuthorizeRequest request, AuthorizeResponse response) {
        TokenRequest tokenRequest = (TokenRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.TOKEN_EXCHANGE)
                .authRequest(request)
                .authResponse(response)
                .account(mAccount)
                .createRequest();

        return tokenRequest;
    }
}