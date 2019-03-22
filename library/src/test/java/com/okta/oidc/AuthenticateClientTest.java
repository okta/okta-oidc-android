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
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.okta.oidc.util.AuthorizationException.TYPE_GENERAL_ERROR;
import static com.okta.oidc.util.AuthorizationException.TYPE_OAUTH_TOKEN_ERROR;
import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthenticateClientTest {
    private static final String CUSTOM_STATE = "CUSTOM_STATE";
    private static final String LOGIN_HINT = "LOGIN_HINT";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String ID_TOKEN = "ID_TOKEN";
    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    private Context mContext;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private HttpConnectionFactory mConnectionFactory;
    private MockEndPoint mEndPoint;

    @Mock
    private OktaStorage mStorage;

    private OIDCAccount mAccount;
    private AuthenticateClient mAuthClient;
    private Gson mGson;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mEndPoint = new MockEndPoint();
        mGson = new Gson();
        String url = mEndPoint.getUrl();
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();
        mAccount = new OIDCAccount.Builder()
                .clientId(CLIENT_ID)
                .redirectUri(url + "callback")
                .endSessionRedirectUri(url + "logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri(url)
                .create();

        ProviderConfiguration configuration = new ProviderConfiguration();
        configuration.issuer = url;
        configuration.revocation_endpoint = url + "revoke";
        configuration.authorization_endpoint = url + "authorize";
        configuration.token_endpoint = url + "token";
        configuration.end_session_endpoint = url + "logout";
        configuration.userinfo_endpoint = url + "userinfo";
        mAccount.setProviderConfig(configuration);

        TokenResponse tokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);
        mAccount.setTokenResponse(tokenResponse);
        mAuthClient = new AuthenticateClient.Builder(mAccount)
                .withState(CUSTOM_STATE)
                .withParameters(Collections.emptyMap())
                .withTabColor(0)
                .callbackExecutor(mExecutor)
                .withStorage(mStorage, mContext)
                .create();
    }

    @After
    public void tearDown() throws Exception {
        mEndPoint.shutDown();
        mExecutor.shutdown();
    }

    @Test
    public void testBuilder() {
        AuthenticateClient.Builder builder = mock(AuthenticateClient.Builder.class);
        AuthenticateClient otherClient = new AuthenticateClient.Builder(mAccount)
                .withState(CUSTOM_STATE)
                .withParameters(Collections.emptyMap())
                .withTabColor(0)
                .callbackExecutor(mExecutor)
                .withStorage(mStorage, mContext)
                .create();
        when(builder.create()).thenReturn(otherClient);

        builder.withState(CUSTOM_STATE);
        verify(builder).withState(CUSTOM_STATE);

        builder.withParameters(Collections.emptyMap());
        verify(builder).withParameters(Collections.emptyMap());

        builder.withTabColor(0);
        verify(builder).withTabColor(0);

        builder.withStorage(mStorage, mContext);
        verify(builder).withStorage(mStorage, mContext);

        builder.withLoginHint(LOGIN_HINT);
        verify(builder).withLoginHint(LOGIN_HINT);

        builder.httpConnectionFactory(mConnectionFactory);
        verify(builder).httpConnectionFactory(mConnectionFactory);

        builder.callbackExecutor(mExecutor);
        verify(builder).callbackExecutor(mExecutor);

        AuthenticateClient client = builder.create();
        verify(builder).create();
        assertEquals(mAuthClient, client);
    }

    @Test
    public void configurationRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueConfigurationSuccess();
        ConfigurationRequest request = mAuthClient.configurationRequest();
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
        ConfigurationRequest request = mAuthClient.configurationRequest();
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
        AuthorizedRequest request = mAuthClient.userProfileRequest();
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
        AuthorizedRequest request = mAuthClient.userProfileRequest();
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertNull(result);
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
    }

    @Test
    public void revokeTokenRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        RevokeTokenRequest request = mAuthClient.revokeTokenRequest("access_token");
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
        RevokeTokenRequest request = mAuthClient.revokeTokenRequest("access_token");
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
        Uri uri = Uri.parse(mAccount.getProviderConfig().userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mAuthClient.authorizedRequest(uri, properties,
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
        Uri uri = Uri.parse(mAccount.getProviderConfig().userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mAuthClient.authorizedRequest(uri, properties,
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

    @Test
    public void logIn() {

    }

    @Test
    public void logOut() {

    }

    @Test
    public void handleAuthorizationResponse() {
    }

    private Map<String, String> toMap(RecordedRequest request) {
        final Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return mGson.fromJson(request.getBody().readUtf8(), mapType);
    }
}