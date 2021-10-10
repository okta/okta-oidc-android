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

import android.text.TextUtils;

import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.DEVICE_SECRET;
import static com.okta.oidc.util.TestValues.ID_TOKEN;
import static com.okta.oidc.util.TestValues.REFRESH_TOKEN;
import static com.okta.oidc.util.TestValues.VALID_EXPIRES_IN;
import static com.okta.oidc.util.TestValues.VALID_SCOPES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class TokensTest {
    private String INVALID_EXPIRES_IN = "INVALID_EXPIRES_IN";

    private String invalidTokenResponsePayload = TestValues.generatePayloadTokenResponse(
            TestValues.ACCESS_TOKEN,
            TestValues.ID_TOKEN,
            TestValues.REFRESH_TOKEN,
            INVALID_EXPIRES_IN,
            TextUtils.join(" ", VALID_SCOPES),
            DEVICE_SECRET);

    @Test
    public void validateEmptyScopeIsAccepted() {
        Tokens tokens = new Tokens(ID_TOKEN,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                VALID_EXPIRES_IN,
                null, System.currentTimeMillis(), DEVICE_SECRET);
        assertNotNull(tokens);
        assertEquals(ACCESS_TOKEN, tokens.getAccessToken());
        assertEquals(REFRESH_TOKEN, tokens.getRefreshToken());
        assertEquals(ID_TOKEN, tokens.getIdToken());
        assertEquals(VALID_EXPIRES_IN, tokens.getExpiresIn());
        assertNull(tokens.getScope());
        assertEquals(DEVICE_SECRET, tokens.getDeviceSecret());
    }

    @Test
    public void validateTokenInit() {
        Tokens tokens = new Tokens(ID_TOKEN,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                VALID_EXPIRES_IN,
                VALID_SCOPES, System.currentTimeMillis(), DEVICE_SECRET);

        assertNotNull(tokens);
        assertEquals(ACCESS_TOKEN, tokens.getAccessToken());
        assertEquals(REFRESH_TOKEN, tokens.getRefreshToken());
        assertEquals(ID_TOKEN, tokens.getIdToken());
        assertEquals(VALID_EXPIRES_IN, tokens.getExpiresIn());
        assertArrayEquals(VALID_SCOPES, tokens.getScope());
        assertEquals(DEVICE_SECRET, tokens.getDeviceSecret());
    }

    @Test
    public void validateTokenInit_fromTokenResponse() {
        TokenResponse tokenResponse = TestValues.getTokenResponse();
        Tokens tokens = new Tokens(tokenResponse);

        assertNotNull(tokens);
        assertEquals(ACCESS_TOKEN, tokens.getAccessToken());
        assertEquals(REFRESH_TOKEN, tokens.getRefreshToken());
        assertEquals(ID_TOKEN, tokens.getIdToken());
        assertEquals(VALID_EXPIRES_IN, tokens.getExpiresIn());
        assertArrayEquals(VALID_SCOPES, tokens.getScope());
    }

    @Test(expected = NumberFormatException.class)
    public void validateTokenInit_fromInvalidTokenResponse() {
        TokenResponse tokenResponse = TokenResponse.RESTORE.restore(invalidTokenResponsePayload);
        new Tokens(tokenResponse);
    }

    @Test
    public void isAccessTokenExpiredSuccess() {
        Tokens tokens = new Tokens(ID_TOKEN,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                VALID_EXPIRES_IN,
                VALID_SCOPES,
                System.currentTimeMillis(),
                DEVICE_SECRET);
        assertFalse(tokens.isAccessTokenExpired());
    }

    @Test
    public void isAccessTokenExpiredFailure() throws InterruptedException {
        Tokens tokens = new Tokens(ID_TOKEN,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                1, //expires in 1 sec
                VALID_SCOPES,
                System.currentTimeMillis(),
                DEVICE_SECRET);
        Thread.sleep(1000);
        assertTrue(tokens.isAccessTokenExpired());
    }
}
