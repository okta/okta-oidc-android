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
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.request.ProviderConfiguration;
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
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthorizeRequestTest {
    private AuthorizeRequest mRequest;

    private OIDCConfig mConfig;
    private String mCodeVerifier;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private ProviderConfiguration mProviderConfig;

    @Before
    public void setUp() {
        mCodeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        mConfig = TestValues.getConfigWithUrl(CUSTOM_URL);
        mProviderConfig = TestValues.getProviderConfiguration(CUSTOM_URL);
        mRequest = new AuthorizeRequest.Builder()
                .authorizeEndpoint(mConfig.getDiscoveryUri().toString())
                .redirectUri(mConfig.getRedirectUri().toString())
                .scope(SCOPES)
                .nonce(CUSTOM_NONCE)
                .clientId(CLIENT_ID)
                .authenticationPayload(new AuthenticationPayload.Builder()
                        .setState(CUSTOM_STATE)
                        .setLoginHint(LOGIN_HINT)
                        .build())
                .providerConfiguration(mProviderConfig)
                .setDisplay(PROMPT)
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
        builder.authorizeEndpoint(mConfig.getDiscoveryUri().toString());
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("redirect_uri missing");
        builder.create();
    }

    @Test
    public void testBuilderFailScope() {
        AuthorizeRequest.Builder builder = new AuthorizeRequest.Builder();
        builder.authorizeEndpoint(mConfig.getDiscoveryUri().toString())
                .redirectUri(mConfig.getRedirectUri().toString());
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("scope missing");
        builder.create();
    }

    @Test
    public void testBuilder() {
        AuthorizeRequest request = new AuthorizeRequest.Builder()
                .authorizeEndpoint(mConfig.getDiscoveryUri().toString())
                .redirectUri(mConfig.getRedirectUri().toString())
                .scope(SCOPES)
                .authenticationPayload(new AuthenticationPayload.Builder()
                        .setState(CUSTOM_STATE)
                        .setLoginHint(LOGIN_HINT)
                        .build())
                .clientId(CLIENT_ID)
                .providerConfiguration(mProviderConfig)
                .setDisplay(PROMPT)
                .nonce(CUSTOM_NONCE)
                .codeVerifier(mCodeVerifier)
                .maxAge(EXPIRES_IN)
                .create();
        assertEquals(request.persist(), mRequest.persist());
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
                mConfig.getRedirectUri().toString());
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
        assertEquals(request.persist(), json);
    }

}