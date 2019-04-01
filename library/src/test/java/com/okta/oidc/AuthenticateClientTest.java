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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;
import com.okta.oidc.util.MockResultCallback;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.okta.oidc.net.request.HttpRequest.Type.TOKEN_EXCHANGE;
import static com.okta.oidc.util.AuthorizationException.TYPE_GENERAL_ERROR;
import static com.okta.oidc.util.AuthorizationException.TYPE_OAUTH_TOKEN_ERROR;
import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.SCOPES;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthenticateClientTest {

    private Context mContext;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private HttpConnectionFactory mConnectionFactory;
    private MockEndPoint mEndPoint;

    private OktaStorage mStorage;

    private OIDCAccount mAccount;
    private AuthenticateClient mAuthClient;
    private SyncAuthenticationClient mSyncAuthClient;
    private Gson mGson;

    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mEndPoint = new MockEndPoint();
        mGson = new Gson();
        mStorage = new SimpleOktaStorage(mContext.getSharedPreferences("OktaTest",
                MODE_PRIVATE));
        String url = mEndPoint.getUrl();
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();

        mAccount = TestValues.getAccountWithUrl(url);
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);


        mAuthClient = new AuthenticateClient.Builder()
                .withAccount(mAccount)
                .withTabColor(0)
                .withCallbackExecutor(mExecutor)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .create();

        mSyncAuthClient = new SyncAuthenticationClient(mConnectionFactory, mAccount,
                0, mStorage, mContext, null);

        mSyncAuthClient.mOktaState.save(mTokenResponse);
        mSyncAuthClient.mOktaState.save(mProviderConfig);
    }

    @After
    public void tearDown() throws Exception {
        mEndPoint.shutDown();
        mExecutor.shutdown();
    }

    @Test
    public void testBuilder() {
        AuthenticateClient.Builder builder = mock(AuthenticateClient.Builder.class);
        AuthenticateClient otherClient = new AuthenticateClient.Builder()
                .withAccount(mAccount)
                .withTabColor(0)
                .withCallbackExecutor(mExecutor)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .create();

        when(builder.create()).thenReturn(otherClient);

        builder.withAccount(mAccount);
        verify(builder).withAccount(mAccount);

        builder.withTabColor(0);
        verify(builder).withTabColor(0);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withCallbackExecutor(mExecutor);
        verify(builder).withCallbackExecutor(mExecutor);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        builder.supportedBrowsers(JsonStrings.FIRE_FOX);
        verify(builder).supportedBrowsers(JsonStrings.FIRE_FOX);

        AuthenticateClient client = builder.create();
        verify(builder).create();
        assertEquals(mAuthClient, client);
    }

    @Test
    public void configurationRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueConfigurationSuccess();
        ConfigurationRequest request = mSyncAuthClient.configurationRequest();
        ProviderConfiguration configuration = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("//.well-known/openid-configuration?client_id=CLIENT_ID"));
        assertEquals(mGson.fromJson(PROVIDER_CONFIG, ProviderConfiguration.class), configuration);
    }

    @Test
    public void configurationRequestFailure() throws AuthorizationException, InterruptedException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueConfigurationFailure();
        ConfigurationRequest request = mSyncAuthClient.configurationRequest();
        ProviderConfiguration configuration = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("//.well-known/openid-configuration?client_id=CLIENT_ID"));
        assertNull(configuration);
    }

    @Test
    public void userProfileRequest() throws InterruptedException, AuthorizationException,
            JSONException {
        mEndPoint.enqueueUserInfoSuccess();
        AuthorizedRequest request = mSyncAuthClient.userProfileRequest();
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(HttpConnection.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNotNull(result);
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }

    @Test
    public void userProfileRequestFailure() throws InterruptedException, AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        AuthorizedRequest request = mSyncAuthClient.userProfileRequest();
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertNull(result);
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
    }

    @Test
    public void revokeTokenRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        RevokeTokenRequest request = mSyncAuthClient.revokeTokenRequest("access_token");
        boolean status = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
        assertTrue(status);
    }

    @Test
    public void revokeTokenRequestFailure() throws AuthorizationException, InterruptedException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        RevokeTokenRequest request = mSyncAuthClient.revokeTokenRequest("access_token");
        boolean status = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertFalse(status);
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
    }

    @Test
    public void authorizedRequest() throws InterruptedException, AuthorizationException,
            JSONException {
        //use userinfo for generic authorized request
        mEndPoint.enqueueUserInfoSuccess();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mSyncAuthClient.authorizedRequest(uri, properties,
                null, HttpConnection.RequestMethod.GET);
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("state"), is(CUSTOM_STATE));
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(HttpConnection.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNotNull(result);
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }

    @Test
    public void authorizedRequestFailure() throws InterruptedException, AuthorizationException {
        //use userinfo for generic authorized request
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mSyncAuthClient.authorizedRequest(uri, properties,
                null, HttpConnection.RequestMethod.GET);
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("state"), is(CUSTOM_STATE));
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(HttpConnection.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNull(result);
    }

    @Test
    public void tokenExchangeFailure() throws InterruptedException, JSONException, AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        String nonce = CodeVerifierUtil.generateRandomState();

        AuthorizeRequest request = new AuthorizeRequest.Builder().codeVerifier(codeVerifier)
                .authorizeEndpoint(mAccount.getDiscoveryUri().toString())
                .redirectUri(mAccount.getRedirectUri().toString())
                .scope(SCOPES)
                .nonce(nonce)
                .create();

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        mEndPoint.enqueueReturnInvalidClient();
        TokenRequest tokenRequest = (TokenRequest) HttpRequestBuilder.newRequest(true)
                .request(TOKEN_EXCHANGE).account(mAccount)
                .providerConfiguration(mProviderConfig)
                .authRequest(request)
                .authResponse(response)
                .createRequest();

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
                .authorizeEndpoint(mAccount.getDiscoveryUri().toString())
                .redirectUri(mAccount.getRedirectUri().toString())
                .scope("openid", "email", "profile")
                .nonce(nonce)
                .create();

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mAccount.getClientId());

        mEndPoint.enqueueTokenSuccess(jws);
        TokenRequest tokenRequest = (TokenRequest) HttpRequestBuilder.newRequest(true)
                .request(TOKEN_EXCHANGE).account(mAccount)
                .authRequest(request)
                .providerConfiguration(mProviderConfig)
                .authResponse(response)
                .createRequest();
        TokenResponse tokenResponse = tokenRequest.executeRequest();

        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/token"));
        assertNotNull(tokenResponse);
        assertEquals(tokenResponse.getIdToken(), jws);
    }

    @Test
    public void getUserProfile() throws InterruptedException, JSONException {
        mEndPoint.enqueueUserInfoSuccess();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<JSONObject, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        mAuthClient.getUserProfile(cb);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        latch.await();
        JSONObject result = cb.getResult();
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(HttpConnection.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNotNull(result);
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }

    @Test
    public void getUserProfileFailure() throws InterruptedException, JSONException {
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<JSONObject, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        mAuthClient.getUserProfile(cb);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertEquals(TYPE_GENERAL_ERROR, cb.getException().type);
    }

    @Test
    public void revokeToken() throws InterruptedException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<Boolean, AuthorizationException> cb = new MockRequestCallback<>(latch);
        mAuthClient.revokeToken("access_token", cb);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        latch.await();
        assertNotNull(cb.getResult());
        assertTrue(cb.getResult());
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
    }

    @Test
    public void revokeTokenFailure() throws InterruptedException {
        mEndPoint.enqueueReturnInvalidClient();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<Boolean, AuthorizationException> cb = new MockRequestCallback<>(latch);
        mAuthClient.revokeToken("access_token", cb);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
        assertEquals(TYPE_OAUTH_TOKEN_ERROR, cb.getException().type);
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
    }

    /*
    TODO move to fragment result tests
        @Test
        public void handleAuthorizationResponseLoginSuccess() {
            mAuthClient.mWebRequest = new AuthorizeRequest.Builder().account(mAccount)
                    .state(CUSTOM_STATE)
                    .create();
            Intent intent = new Intent();
            intent.setData(Uri.parse("com.okta.test:/authorize?state=CUSTOM_STATE"));
            MockResultCallback<Boolean, AuthorizationException> cb = new MockResultCallback<>();
            boolean exchange = mAuthClient.handleAuthorizationResponse(
                    AuthenticateClient.REQUEST_CODE_SIGN_IN, RESULT_OK, intent, cb);
            assertTrue(exchange);
        }

        @Test
        public void handleAuthorizationResponseLoginFailed() {
            AuthenticateClient.sResultHandled = false;
            mAuthClient.mWebRequest = new AuthorizeRequest.Builder().account(mAccount)
                    .state(CUSTOM_STATE)
                    .create();
            Intent intent = new Intent();
            intent.setData(Uri.parse("com.okta.test:/authorize?state=MISMATCH_STATE"));
            MockResultCallback<Boolean, AuthorizationException> cb = new MockResultCallback<>();
            boolean exchange = mAuthClient.handleAuthorizationResponse(
                    AuthenticateClient.REQUEST_CODE_SIGN_IN, RESULT_OK, intent, cb);
            assertFalse(exchange);
            assertNotNull(cb.getException());
            assertEquals("Mismatch states", cb.getError());
        }

        @Test
        public void handleAuthorizationResponseLogoutSuccess() {
            AuthenticateClient.sResultHandled = false;
            mAuthClient.mWebRequest = new LogoutRequest.Builder().account(mAccount)
                    .state(CUSTOM_STATE)
                    .create();
            Intent intent = new Intent();
            intent.setData(Uri.parse("com.okta.test:/logout?state=" + CUSTOM_STATE));
            MockResultCallback<Boolean, AuthorizationException> cb = new MockResultCallback<>();
            mAuthClient.handleAuthorizationResponse(AuthenticateClient.REQUEST_CODE_SIGN_OUT, RESULT_OK,
                    intent, cb);
            assertTrue(cb.getResult());
            assertNull(cb.getException());
        }

        @Test
        public void handleAuthorizationResponseLogoutFailed() {
            AuthenticateClient.sResultHandled = false;
            mAuthClient.mWebRequest = new LogoutRequest.Builder().account(mAccount)
                    .state(CUSTOM_STATE)
                    .create();
            Intent intent = new Intent();
            intent.setData(Uri.parse("com.okta.test:/logout?state=MISMATCH_STATE"));
            MockResultCallback<Boolean, AuthorizationException> cb = new MockResultCallback<>();
            mAuthClient.handleAuthorizationResponse(AuthenticateClient.REQUEST_CODE_SIGN_OUT, RESULT_OK,
                    intent, cb);
            assertNull(cb.getResult());
            assertNotNull(cb.getException());
            assertEquals("Mismatch states", cb.getError());
        }
    */
    private Map<String, String> toMap(RecordedRequest request) {
        final Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return mGson.fromJson(request.getBody().readUtf8(), mapType);
    }
}