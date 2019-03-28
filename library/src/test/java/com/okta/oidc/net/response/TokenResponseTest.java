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
package com.okta.oidc.net.response;

import com.google.gson.Gson;
import com.okta.oidc.util.AsciiStringListUtil;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.okta.oidc.net.response.TokenResponse.RESTORE;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.EXPIRES_IN;
import static com.okta.oidc.util.TestValues.ID_TOKEN;
import static com.okta.oidc.util.TestValues.REFRESH_TOKEN;
import static com.okta.oidc.util.TestValues.SCOPES;
import static com.okta.oidc.util.TestValues.TYPE_BEARER;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class TokenResponseTest {
    private TokenResponse mToken;

    @Before
    public void setUp() throws Exception {
        mToken = RESTORE.restore(TOKEN_RESPONSE);
    }

    @Test
    public void getAccessToken() {
        assertEquals(mToken.getAccessToken(), ACCESS_TOKEN);
    }

    @Test
    public void getTokenType() {
        assertEquals(mToken.getTokenType(), TYPE_BEARER);
    }

    @Test
    public void getExpiresIn() {
        assertEquals(mToken.getExpiresIn(), EXPIRES_IN);
    }

    @Test
    public void getScope() {
        assertEquals(mToken.getScope(),
                AsciiStringListUtil.iterableToString(Arrays.asList(SCOPES)));
    }

    @Test
    public void getRefreshToken() {
        assertEquals(mToken.getRefreshToken(), REFRESH_TOKEN);
    }

    @Test
    public void getIdToken() {
        assertEquals(mToken.getIdToken(), ID_TOKEN);
    }

    @Test
    public void getKey() {
        assertEquals(mToken.getKey(), RESTORE.getKey());
    }

    @Test
    public void persist() {
        String json = mToken.persist();
        TokenResponse tokenResponse = new Gson().fromJson(json, TokenResponse.class);
        assertEquals(tokenResponse, mToken);
    }

    @Test
    public void encrypt() {
        assertTrue(mToken.encrypt());
    }

    @Test
    public void equals() {
        TokenResponse other = RESTORE.restore(TOKEN_RESPONSE);
        assertEquals(mToken, other);
    }
}