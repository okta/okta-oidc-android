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

package com.okta.oidc.clients.web;

import android.content.Context;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.mockwebserver.RecordedRequest;

import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.SCOPES;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class SyncWebAuthClientTest {

    private Context mContext;
    private HttpConnectionFactory mConnectionFactory;
    private OIDCConfig mAccount;
    private OktaStorage mStorage;
    private SyncWebAuthClientImpl mSyncWebAuth;
    private MockEndPoint mEndPoint;
    private Gson mGson;
    private OktaState mOktaState;
    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();


        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();
        mAccount = TestValues.getAccountWithUrl(url);
        mStorage = new SimpleOktaStorage(mContext);
        mGson = new Gson();

        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        SyncWebAuthClient okta = new Okta.SyncWebAuthBuilder()
                .withConfig(mAccount)
                .withHttpConnectionFactory(mConnectionFactory)
                .withContext(mContext)
                .withStorage(mStorage)
                .create();


        mSyncWebAuth = (SyncWebAuthClientImpl)okta;

        mOktaState = mSyncWebAuth.getOktaState();
        mOktaState.save(mProviderConfig);
    }

    @Test
    public void configurationRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueConfigurationSuccess();
        ConfigurationRequest request = mSyncWebAuth.configurationRequest();
        ProviderConfiguration configuration = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("//.well-known/openid-configuration?client_id=CLIENT_ID"));
        assertNotNull(configuration);
        assertEquals(mGson.fromJson(PROVIDER_CONFIG, ProviderConfiguration.class).persist(),
                configuration.persist());
    }

    @Test
    public void configurationRequestFailure() throws AuthorizationException, InterruptedException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueConfigurationFailure();
        ConfigurationRequest request = mSyncWebAuth.configurationRequest();
        ProviderConfiguration configuration = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("//.well-known/openid-configuration?client_id=CLIENT_ID"));
        assertNull(configuration);
    }

    @Test
    public void tokenExchangeFailure() throws InterruptedException, JSONException, AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        String nonce = CodeVerifierUtil.generateRandomState();

        AuthorizeRequest request = new AuthorizeRequest.Builder().codeVerifier(codeVerifier)
                .authorizeEndpoint(mProviderConfig.authorization_endpoint)
                .redirectUri(mAccount.getRedirectUri().toString())
                .scope(SCOPES)
                .nonce(nonce)
                .create();

        mOktaState.save(request);

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        mEndPoint.enqueueReturnInvalidClient();
        TokenRequest tokenRequest = mSyncWebAuth.tokenExchange(response);
        TokenResponse tokenResponse = tokenRequest.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/token"));
        assertNull(tokenResponse);
    }

    @Test
    public void tokenExchangeSuccess() throws InterruptedException, JSONException, AuthorizationException {
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        String nonce = CodeVerifierUtil.generateRandomState();

        AuthorizeRequest request = new AuthorizeRequest.Builder().codeVerifier(codeVerifier)
                .authorizeEndpoint(mProviderConfig.authorization_endpoint)
                .redirectUri(mAccount.getRedirectUri().toString())
                .scope("openid", "email", "profile")
                .nonce(nonce)
                .create();

        mOktaState.save(request);

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mAccount.getClientId());

        mEndPoint.enqueueTokenSuccess(jws);
        TokenRequest tokenRequest = mSyncWebAuth.tokenExchange(response);
        TokenResponse tokenResponse = tokenRequest.executeRequest();

        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/token"));
        assertNotNull(tokenResponse);
        assertEquals(tokenResponse.getIdToken(), jws);
    }
}
