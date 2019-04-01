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
package com.okta.oidc.net.request;

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.response.TokenResponse;
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

import java.util.Collections;

import static com.okta.oidc.net.request.HttpRequest.Type.AUTHORIZED;
import static com.okta.oidc.net.request.HttpRequest.Type.PROFILE;
import static com.okta.oidc.net.request.HttpRequest.Type.REVOKE_TOKEN;
import static com.okta.oidc.net.request.HttpRequest.Type.TOKEN_EXCHANGE;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_CODE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.getAuthorizeRequest;
import static com.okta.oidc.util.TestValues.getAuthorizeResponse;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HttpRequestBuilderTest {
    private OIDCAccount mAccount;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private TokenResponse mTokenResponse;
    private ProviderConfiguration mConfiguration;

    @Before
    public void setUp() {
        mConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mTokenResponse = new Gson().fromJson(JsonStrings.TOKEN_RESPONSE, TokenResponse.class);
        mAccount = TestValues.getAccountWithUrl(TestValues.CUSTOM_URL);
    }

    @Test
    public void newRequest() {
        assertNotNull(HttpRequestBuilder.newRequest(false));
    }

    @Test
    public void createWithNoAccountRequest() {
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Invalid account");
        HttpRequestBuilder.newRequest(false).createRequest();
    }

    @Test
    public void createInvalidConfigurationRequest() {
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Invalid account");
        HttpRequestBuilder.newRequest(false)
                .request(HttpRequest.Type.CONFIGURATION)
                .createRequest();
    }

    @Test
    public void createConfigurationRequest() {
        ConfigurationRequest request = (ConfigurationRequest) HttpRequestBuilder.newRequest(false)
                .request(HttpRequest.Type.CONFIGURATION)
                .account(mAccount)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.CONFIGURATION.toString()));
    }

    @Test
    public void createInvalidTokenExchangeRequest() {
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Missing service configuration");
        HttpRequestBuilder.newRequest(false)
                .request(TOKEN_EXCHANGE)
                .account(mAccount)
                .createRequest();
    }

    @Test
    public void createInvalidTokenExchangeRequestNoAuth() {
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Missing auth request or response");
        HttpRequestBuilder.newRequest(false)
                .providerConfiguration(mConfiguration)
                .request(TOKEN_EXCHANGE)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .account(mAccount)
                .createRequest();
    }

    @Test
    public void createTokenExchangeRequest() {
        TokenRequest request = (TokenRequest) HttpRequestBuilder.newRequest(false)
                .providerConfiguration(mConfiguration)
                .request(HttpRequest.Type.TOKEN_EXCHANGE)
                .authRequest(getAuthorizeRequest(mAccount,
                        CodeVerifierUtil.generateRandomCodeVerifier()))
                .authResponse(getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE))
                .account(mAccount)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.TOKEN_EXCHANGE.toString()));
    }

    @Test
    public void createInvalidAuthorizedRequest() {
        ProviderConfiguration configuration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Not logged in or invalid uri");
        HttpRequestBuilder.newRequest(false)
                .providerConfiguration(configuration)
                .request(AUTHORIZED)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .account(mAccount)
                .createRequest();
    }

    @Test
    public void createAuthorizedRequest() {
        AuthorizedRequest request = (AuthorizedRequest) HttpRequestBuilder.newRequest(true)
                .providerConfiguration(mConfiguration)
                .tokenResponse(mTokenResponse)
                .request(HttpRequest.Type.AUTHORIZED)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .uri(Uri.parse(CUSTOM_URL))
                .account(mAccount)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.AUTHORIZED.toString()));
    }

    @Test
    public void createInvalidProfileRequest() {
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Not logged in");
        HttpRequestBuilder.newRequest(false)
                .providerConfiguration(mConfiguration)
                .request(PROFILE)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .account(mAccount)
                .createRequest();
    }

    @Test
    public void createProfileRequest() {
        AuthorizedRequest request = (AuthorizedRequest) HttpRequestBuilder.newRequest(true)
                .providerConfiguration(mConfiguration)
                .tokenResponse(mTokenResponse)
                .request(HttpRequest.Type.PROFILE)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .account(mAccount)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.PROFILE.toString()));
    }

    @Test
    public void createInvalidRevokeRequest() {
        mExpectedEx.expect(IllegalStateException.class);
        mExpectedEx.expectMessage("Invalid token");
        HttpRequestBuilder.newRequest(true)
                .providerConfiguration(mConfiguration)
                .request(REVOKE_TOKEN)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .account(mAccount)
                .createRequest();
    }

    @Test
    public void createRevokeRequest() {
        RevokeTokenRequest request = (RevokeTokenRequest) HttpRequestBuilder.newRequest(true)
                .request(HttpRequest.Type.REVOKE_TOKEN)
                .providerConfiguration(mConfiguration)
                .tokenResponse(mTokenResponse)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .tokenToRevoke(ACCESS_TOKEN)
                .account(mAccount)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.REVOKE_TOKEN.toString()));
    }

    @Test
    public void request() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(true)
                .request(REVOKE_TOKEN);
        assertEquals(builder.mRequestType, REVOKE_TOKEN);
    }

    @Test
    public void connectionFactory() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(true)
                .connectionFactory(new HttpConnection.DefaultConnectionFactory());
        assertNotNull(builder.mConn);
    }

    @Test
    public void account() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .account(mAccount);
        assertNotNull(builder.mAccount);
    }

    @Test
    public void authRequest() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .authRequest(TestValues.getAuthorizeRequest(mAccount,
                        CodeVerifierUtil.generateRandomCodeVerifier()));
        assertNotNull(builder.mAuthRequest);
    }

    @Test
    public void authResponse() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .authResponse(TestValues.getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE));
        assertNotNull(builder.mAuthResponse);
    }

    @Test
    public void postParameters() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .postParameters(Collections.EMPTY_MAP);
        assertNotNull(builder.mPostParameters);
    }

    @Test
    public void properties() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .properties(Collections.EMPTY_MAP);
        assertNotNull(builder.mProperties);
    }

    @Test
    public void uri() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .uri(Uri.parse(CUSTOM_URL));
        assertNotNull(builder.mUri);
    }

    @Test
    public void httpRequestMethod() {
        HttpRequestBuilder builder = HttpRequestBuilder.newRequest(false)
                .httpRequestMethod(HttpConnection.RequestMethod.GET);
        assertNotNull(builder.mRequestMethod);
    }
}