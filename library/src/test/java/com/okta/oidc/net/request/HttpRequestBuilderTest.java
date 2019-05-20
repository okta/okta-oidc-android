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
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.TokenResponse;
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

import java.util.Collections;

import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_CODE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.getAuthorizeRequest;
import static com.okta.oidc.util.TestValues.getAuthorizeResponse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HttpRequestBuilderTest {
    private OIDCConfig mConfig;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private TokenResponse mTokenResponse;
    private ProviderConfiguration mConfiguration;

    @Before
    public void setUp() {
        mConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mTokenResponse = new Gson().fromJson(JsonStrings.TOKEN_RESPONSE, TokenResponse.class);
        mConfig = TestValues.getConfigWithUrl(TestValues.CUSTOM_URL);
    }

    @Test
    public void newRequest() {
        assertNotNull(HttpRequestBuilder.newAuthorizedRequest());
        assertNotNull(HttpRequestBuilder.newConfigurationRequest());
        assertNotNull(HttpRequestBuilder.newTokenRequest());
        assertNotNull(HttpRequestBuilder.newRevokeTokenRequest());
        assertNotNull(HttpRequestBuilder.newProfileRequest());
        assertNotNull(HttpRequestBuilder.newRefreshTokenRequest());
        assertNotNull(HttpRequestBuilder.newIntrospectRequest());
    }

    @Test
    public void createWithNoConfigRequest() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Invalid config");
        HttpRequestBuilder.newAuthorizedRequest().createRequest();
    }

    @Test
    public void createInvalidConfigurationRequest() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Invalid config");
        HttpRequestBuilder.newConfigurationRequest().createRequest();
    }

    @Test
    public void createConfigurationRequest() throws AuthorizationException {
        ConfigurationRequest request = HttpRequestBuilder.newConfigurationRequest()
                .config(mConfig)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.CONFIGURATION.toString()));
    }

    @Test
    public void createInvalidTokenExchangeRequest() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Missing service configuration");
        HttpRequestBuilder.newTokenRequest()
                .config(mConfig)
                .createRequest();
    }

    @Test
    public void createInvalidTokenExchangeRequestNoAuth() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Missing auth request or response");
        HttpRequestBuilder.newTokenRequest()
                .providerConfiguration(mConfiguration)
                .config(mConfig)
                .createRequest();
    }

    @Test
    public void createTokenExchangeRequest() throws AuthorizationException {
        TokenRequest request = HttpRequestBuilder.newTokenRequest()
                .providerConfiguration(mConfiguration)
                .authRequest(getAuthorizeRequest(mConfig,
                        CodeVerifierUtil.generateRandomCodeVerifier()))
                .authResponse(getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE))
                .config(mConfig)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.TOKEN_EXCHANGE.toString()));
    }

    @Test
    public void createInvalidAuthorizedRequest() throws AuthorizationException {
        ProviderConfiguration configuration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Not logged in or invalid uri");
        HttpRequestBuilder.newAuthorizedRequest()
                .providerConfiguration(configuration)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .config(mConfig)
                .createRequest();
    }

    @Test
    public void createAuthorizedRequest() throws AuthorizationException {
        AuthorizedRequest request = HttpRequestBuilder.newAuthorizedRequest()
                .providerConfiguration(mConfiguration)
                .tokenResponse(mTokenResponse)
                .httpRequestMethod(HttpConnection.RequestMethod.GET)
                .uri(Uri.parse(CUSTOM_URL))
                .config(mConfig)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.AUTHORIZED.toString()));
    }

    @Test
    public void createInvalidProfileRequest() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Not logged in");
        HttpRequestBuilder.newProfileRequest()
                .providerConfiguration(mConfiguration)
                .config(mConfig)
                .createRequest();
    }

    @Test
    public void createProfileRequest() throws AuthorizationException {
        AuthorizedRequest request = HttpRequestBuilder.newProfileRequest()
                .providerConfiguration(mConfiguration)
                .tokenResponse(mTokenResponse)
                .config(mConfig)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.AUTHORIZED.toString()));
    }

    @Test
    public void createInvalidRevokeRequest() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Invalid token");
        HttpRequestBuilder.newRevokeTokenRequest()
                .providerConfiguration(mConfiguration)
                .config(mConfig)
                .createRequest();
    }

    @Test
    public void createRevokeRequest() throws AuthorizationException {
        RevokeTokenRequest request = HttpRequestBuilder.newRevokeTokenRequest()
                .providerConfiguration(mConfiguration)
                .tokenToRevoke(mTokenResponse.getAccessToken())
                .tokenToRevoke(ACCESS_TOKEN)
                .config(mConfig)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.REVOKE_TOKEN.toString()));
    }

    @Test
    public void createIntrospectRequest() throws AuthorizationException {
        IntrospectRequest request = HttpRequestBuilder.newIntrospectRequest()
                .providerConfiguration(mConfiguration)
                .introspect(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN)
                .config(mConfig)
                .createRequest();
        assertNotNull(request);
        assertTrue(request.toString().contains(HttpRequest.Type.INTROSPECT.toString()));
    }

    @Test
    public void connectionFactory() {
        HttpRequestBuilder.Authorized builder = HttpRequestBuilder.newAuthorizedRequest()
                .connectionFactory(new HttpConnection.DefaultConnectionFactory());
        assertNotNull(builder.mConn);
    }

    @Test
    public void config() {
        HttpRequestBuilder.Authorized builder = HttpRequestBuilder.newAuthorizedRequest()
                .config(mConfig);
        assertNotNull(builder.mConfig);
    }

    @Test
    public void authRequest() throws AuthorizationException {
        HttpRequestBuilder.TokenExchange builder = HttpRequestBuilder.newTokenRequest()
                .authRequest(TestValues.getAuthorizeRequest(mConfig,
                        CodeVerifierUtil.generateRandomCodeVerifier()));
        assertNotNull(builder.mAuthRequest);
    }

    @Test
    public void authResponse() {
        HttpRequestBuilder.TokenExchange builder = HttpRequestBuilder.newTokenRequest()
                .authResponse(TestValues.getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE));
        assertNotNull(builder.mAuthResponse);
    }

    @Test
    public void postParameters() {
        HttpRequestBuilder.Authorized builder = HttpRequestBuilder.newAuthorizedRequest()
                .postParameters(Collections.EMPTY_MAP);
        assertNotNull(builder.mPostParameters);
    }

    @Test
    public void properties() {
        HttpRequestBuilder.Authorized builder = HttpRequestBuilder.newAuthorizedRequest()
                .properties(Collections.EMPTY_MAP);
        assertNotNull(builder.mProperties);
    }

    @Test
    public void uri() {
        HttpRequestBuilder.Authorized builder = HttpRequestBuilder.newAuthorizedRequest()
                .uri(Uri.parse(CUSTOM_URL));
        assertNotNull(builder.mUri);
    }

    @Test
    public void httpRequestMethod() {
        HttpRequestBuilder.Authorized builder = HttpRequestBuilder.newAuthorizedRequest()
                .httpRequestMethod(HttpConnection.RequestMethod.GET);
        assertNotNull(builder.mRequestMethod);
    }
}