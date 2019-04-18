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

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.CUSTOM_CODE;
import static com.okta.oidc.util.TestValues.CUSTOM_NONCE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.getAuthorizeRequest;
import static com.okta.oidc.util.TestValues.getAuthorizeResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)

public class OktaIdTokenTest {
    private OIDCConfig mAccount;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private ProviderConfiguration mConfiguration;

    @Before
    public void setUp() throws Exception {
        mAccount = TestValues.getAccountWithUrl(CUSTOM_URL);
        mConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
    }

    @Test
    public void validate() throws AuthorizationException {
        String jwt = TestValues.getJwt(CUSTOM_URL, CUSTOM_NONCE, mAccount.getClientId(),
                "fakeaud");
        OktaIdToken idToken = OktaIdToken.parseIdToken(jwt);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mAccount, getAuthorizeRequest(mAccount, verifier),
                        getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE), mConfiguration);
        idToken.validate(tokenRequest, System::currentTimeMillis);
        assertNotNull(idToken);
        assertNotNull(idToken.mHeader);
        assertNotNull(idToken.mClaims);
    }

    @Test
    public void validateInvalidNonce() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        String jwt = TestValues.getJwt(CUSTOM_URL, "invalid", mAccount.getClientId(),
                "fakeaud");
        OktaIdToken idToken = OktaIdToken.parseIdToken(jwt);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mAccount, getAuthorizeRequest(mAccount, verifier),
                        getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE), mConfiguration);
        idToken.validate(tokenRequest, System::currentTimeMillis);
    }

    @Test
    public void validateExpiredToken() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        String jws = TestValues.getExpiredJwt(CUSTOM_URL, CUSTOM_NONCE, mAccount.getClientId());
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();
        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mAccount, getAuthorizeRequest(mAccount, verifier),
                        getAuthorizeResponse("state", "code"), mConfiguration);
        idToken.validate(tokenRequest, System::currentTimeMillis);
    }

    @Test
    public void validateIssuedAtTimeout() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        OktaIdToken token = OktaIdToken.parseIdToken(JsonStrings.VALID_ID_TOKEN);

        String jws = TestValues.getJwtIssuedAtTimeout(CUSTOM_URL, CUSTOM_NONCE,
                mAccount.getClientId());
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mAccount, getAuthorizeRequest(mAccount, verifier),
                        getAuthorizeResponse("state", "code"), mConfiguration);
        idToken.validate(tokenRequest, System::currentTimeMillis);
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
    public void parseInvalidIdToken() {
        mExpectedEx.expect(IllegalArgumentException.class);
        OktaIdToken.parseIdToken(JsonStrings.INVALID_ID_TOKEN);
    }
}