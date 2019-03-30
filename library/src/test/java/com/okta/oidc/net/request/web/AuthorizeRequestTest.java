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
package com.okta.oidc.net.request.web;

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.util.AsciiStringListUtil;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.okta.oidc.util.TestValues.CLIENT_ID;
import static com.okta.oidc.util.TestValues.CUSTOM_NONCE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.EXPIRES_IN;
import static com.okta.oidc.util.TestValues.LOGIN_HINT;
import static com.okta.oidc.util.TestValues.PROMPT;
import static com.okta.oidc.util.TestValues.SCOPES;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthorizeRequestTest {
    private AuthorizeRequest mRequest;

    private OIDCAccount mAccount;
    private String mCodeVerifier;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        mCodeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        mAccount = TestValues.getAccountWithUrl(CUSTOM_URL);
        mAccount.setProviderConfig(TestValues.getProviderConfiguration(CUSTOM_URL));
        mRequest = new AuthorizeRequest.Builder()
                .authorizeEndpoint(mAccount.getDiscoveryUri().toString())
                .redirectUri(mAccount.getRedirectUri().toString())
                .scope(SCOPES)
                .nonce(CUSTOM_NONCE)
                .clientId(CLIENT_ID)
                .state(CUSTOM_STATE)
                .setDisplay(PROMPT)
                .loginHint(LOGIN_HINT)
                .codeVerifier(mCodeVerifier)
                .maxAge(EXPIRES_IN)
                .create();
    }

    @Test
    public void testBuilderFailEndpoint() {
        AuthorizeRequest.Builder builder = new AuthorizeRequest.Builder();
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("authorize_endpoint missing");
        builder.create();
    }

    @Test
    public void testBuilderFailRedirectUri() {
        AuthorizeRequest.Builder builder = new AuthorizeRequest.Builder();
        builder.authorizeEndpoint(mAccount.getDiscoveryUri().toString());
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("redirect_uri missing");
        builder.create();
    }

    @Test
    public void testBuilderFailScope() {
        AuthorizeRequest.Builder builder = new AuthorizeRequest.Builder();
        builder.authorizeEndpoint(mAccount.getDiscoveryUri().toString())
                .redirectUri(mAccount.getRedirectUri().toString());
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("scope missing");
        builder.create();
    }

    @Test
    public void testBuilder() {
        AuthorizeRequest request = new AuthorizeRequest.Builder()
                .authorizeEndpoint(mAccount.getDiscoveryUri().toString())
                .redirectUri(mAccount.getRedirectUri().toString())
                .scope(SCOPES)
                .state(CUSTOM_STATE)
                .clientId(CLIENT_ID)
                .setDisplay(PROMPT)
                .nonce(CUSTOM_NONCE)
                .loginHint(LOGIN_HINT)
                .codeVerifier(mCodeVerifier)
                .maxAge(EXPIRES_IN)
                .create();
        assertEquals(request, mRequest);
    }

    @Test
    public void getState() {
        assertEquals(mRequest.getState(), CUSTOM_STATE);
    }

    @Test
    public void getCodeVerifier() {
        assertEquals(mRequest.getCodeVerifier(), mCodeVerifier);
    }

    @Test
    public void getNonce() {
        assertEquals(mRequest.getNonce(), CUSTOM_NONCE);
    }

    @Test
    public void toUri() {
        Uri uri = mRequest.toUri();
        assertEquals(uri.getQueryParameter("redirect_uri"),
                mAccount.getRedirectUri().toString());
        assertEquals(uri.getQueryParameter("scope"),
                AsciiStringListUtil.iterableToString(Arrays.asList(SCOPES)));
        assertEquals(uri.getQueryParameter("nonce"), CUSTOM_NONCE);
        assertEquals(uri.getQueryParameter("state"), CUSTOM_STATE);
    }

    @Test
    public void getKey() {
        assertEquals(mRequest.getKey(), "WebRequest");
    }

    @Test
    public void persist() {
        String json = mRequest.persist();
        AuthorizeRequest.Parameters parameters = new Gson().fromJson(json, AuthorizeRequest.Parameters.class);
        AuthorizeRequest request = new AuthorizeRequest(parameters);
        assertEquals(request, mRequest);
    }

    @Test
    public void encrypt() {
        assertFalse(mRequest.encrypt());
    }
}