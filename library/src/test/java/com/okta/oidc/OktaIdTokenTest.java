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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)

public class OktaIdTokenTest {
    private OIDCConfig mConfig;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private ProviderConfiguration mConfiguration;

    @Before
    public void setUp() throws Exception {
        mConfig = TestValues.getConfigWithUrl(CUSTOM_URL);
        mConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
    }

    @Test
    public void validate() throws AuthorizationException {
        String jwt = TestValues.getJwt(CUSTOM_URL, CUSTOM_NONCE, mConfig.getClientId(),
                "fakeaud");
        OktaIdToken idToken = OktaIdToken.parseIdToken(jwt);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE), mConfiguration);
        idToken.validate(tokenRequest, new OktaIdToken.DefaultValidator(System::currentTimeMillis));
        assertNotNull(idToken);
        assertNotNull(idToken.mHeader);
        assertNotNull(idToken.mClaims);
    }

    @Test
    public void validateWithCustomValidatorThatAlwaysThrowsException() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);

        String jwt = TestValues.getJwt(CUSTOM_URL, CUSTOM_NONCE, mConfig.getClientId(),
                "fakeaud");
        OktaIdToken idToken = OktaIdToken.parseIdToken(jwt);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE), mConfiguration);
        idToken.validate(tokenRequest, oktaIdToken -> {
            throw new AuthorizationException("Expected", null);
        });
    }

    @Test
    public void validateInvalidNonce() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        String jwt = TestValues.getJwt(CUSTOM_URL, "invalid", mConfig.getClientId(),
                "fakeaud");
        OktaIdToken idToken = OktaIdToken.parseIdToken(jwt);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE), mConfiguration);
        idToken.validate(tokenRequest, new OktaIdToken.DefaultValidator(System::currentTimeMillis));
    }

    @Test
    public void validateExpiredToken() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        String jws = TestValues.getExpiredJwt(CUSTOM_URL, CUSTOM_NONCE, mConfig.getClientId());
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();
        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse("state", "code"), mConfiguration);
        idToken.validate(tokenRequest, new OktaIdToken.DefaultValidator(System::currentTimeMillis));
    }

    @Test
    public void validateExpiredTokenWithEmptyValidator() throws AuthorizationException {
        String jws = TestValues.getExpiredJwt(CUSTOM_URL, CUSTOM_NONCE, mConfig.getClientId());
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();
        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse("state", "code"), mConfiguration);
        OktaIdToken.Validator validator = mock(OktaIdToken.Validator.class);
        idToken.validate(tokenRequest, validator);
        verify(validator).validate(idToken);
    }

    @Test
    public void validateIssuedAtTimeout() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);

        String jws = TestValues.getJwtIssuedAtTimeout(CUSTOM_URL, CUSTOM_NONCE,
                mConfig.getClientId());
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse("state", "code"), mConfiguration);
        idToken.validate(tokenRequest, new OktaIdToken.DefaultValidator(System::currentTimeMillis));
    }

    @Test
    public void validateIssuedAtTimeoutWithEmptyValidator() throws AuthorizationException {
        String jws = TestValues.getJwtIssuedAtTimeout(CUSTOM_URL, CUSTOM_NONCE,
                mConfig.getClientId());
        OktaIdToken idToken = OktaIdToken.parseIdToken(jws);
        String verifier = CodeVerifierUtil.generateRandomCodeVerifier();

        TokenRequest tokenRequest =
                TestValues.getTokenRequest(mConfig, getAuthorizeRequest(mConfig, verifier),
                        getAuthorizeResponse("state", "code"), mConfiguration);
        OktaIdToken.Validator validator = mock(OktaIdToken.Validator.class);
        idToken.validate(tokenRequest, validator);
        verify(validator).validate(idToken);
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
