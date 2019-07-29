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

package com.okta.oidc.clients.sessions;

import android.content.Context;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.web.SyncWebAuthClient;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.EncryptionManagerStub;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.HttpClientFactory;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import okhttp3.mockwebserver.RecordedRequest;
import okio.BufferedSource;

import static com.okta.oidc.net.ConnectionParameters.CONTENT_TYPE;
import static com.okta.oidc.net.ConnectionParameters.DEFAULT_ENCODING;
import static com.okta.oidc.net.ConnectionParameters.JSON_CONTENT_TYPE;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class SyncSessionClientImplTest {

    private Context mContext;
    private OIDCConfig mConfig;
    private HttpClientFactory mClientFactory;
    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
    private SyncSessionClientImpl mSyncSessionClientImpl;
    private MockEndPoint mEndPoint;
    private Gson mGson;
    private OktaState mOktaState;
    private OktaHttpClient mHttpClient;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();
    private final int mClientType;

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HttpClientFactory.USE_DEFAULT_HTTP},
                {HttpClientFactory.USE_OK_HTTP},
                {HttpClientFactory.USE_SYNC_OK_HTTP}});
    }

    public SyncSessionClientImplTest(int clientType) {
        mClientType = clientType;
    }

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mConfig = TestValues.getConfigWithUrl(url);
        mGson = new Gson();
        OktaStorage storage = new SharedPreferenceStorage(mContext);

        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);
        mHttpClient = mClientFactory.build();

        SyncWebAuthClient mSyncWebAuth = new Okta.SyncWebAuthBuilder()
                .withConfig(mConfig)
                .withOktaHttpClient(mHttpClient)
                .withContext(mContext)
                .withStorage(storage)
                .withEncryptionManager(new EncryptionManagerStub())
                .create();

        mSyncSessionClientImpl = (SyncSessionClientImpl) mSyncWebAuth.getSessionClient();

        mOktaState = mSyncSessionClientImpl.getOktaState();

        mOktaState.save(mProviderConfig);
    }

    @After
    public void tearDown() throws Exception {
        mEndPoint.shutDown();
    }


    @Test
    public void clear_success() throws AuthorizationException, OktaRepository.EncryptionException {
        mOktaState.save(mProviderConfig);
        mOktaState.save(mTokenResponse);
        mOktaState.save(TestValues.getAuthorizeRequest(mConfig, null));

        mSyncSessionClientImpl.clear();

        assertNull(mOktaState.getAuthorizeRequest());
        assertNull(mOktaState.getProviderConfiguration());
        assertNull(mOktaState.getTokenResponse());
        assertFalse(mSyncSessionClientImpl.isAuthenticated());
        assertNull(mSyncSessionClientImpl.getTokens());
    }

    @Test
    public void isLoggedIn_success() throws AuthorizationException, OktaRepository.EncryptionException {
        mOktaState.save(TestValues.getTokenResponse());

        boolean result = mSyncSessionClientImpl.isAuthenticated();

        assertTrue(result);
        assertNotNull(mSyncSessionClientImpl.getTokens());
    }

    @Test
    public void isLoggedIn_false() throws AuthorizationException {
        boolean result = mSyncSessionClientImpl.isAuthenticated();

        assertFalse(result);
        assertNull(mSyncSessionClientImpl.getTokens());
    }

    @Test
    public void refreshTokenRequest() throws InterruptedException, JSONException, AuthorizationException, OktaRepository.EncryptionException {
        mOktaState.save(mTokenResponse);
        RefreshTokenRequest request = mSyncSessionClientImpl.refreshTokenRequest(mOktaState.getProviderConfiguration(), mTokenResponse);
        String nonce = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mConfig.getClientId());
        mEndPoint.enqueueTokenSuccess(jws);
        TokenResponse response = request.executeRequest(mHttpClient);
        assertNotNull(response);

        TokenResponse original = mGson.fromJson(String.format(TOKEN_SUCCESS, jws),
                TokenResponse.class);
        assertEquals(original.getIdToken(), response.getIdToken());
        assertEquals(original.getRefreshToken(), response.getRefreshToken());
        assertEquals(original.getIdToken(), response.getIdToken());
    }

    @Test
    public void refreshTokenRequestFailure() throws InterruptedException, JSONException, AuthorizationException, OktaRepository.EncryptionException {
        mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        RefreshTokenRequest request = mSyncSessionClientImpl.refreshTokenRequest(mOktaState.getProviderConfiguration(), mTokenResponse);
        TokenResponse response = request.executeRequest(mHttpClient);
        assertNull(response);
    }

    @Test
    public void userProfileRequest() throws InterruptedException,
            JSONException, AuthorizationException, OktaRepository.EncryptionException {
        mOktaState.save(mTokenResponse);
        mEndPoint.enqueueUserInfoSuccess();
        AuthorizedRequest request = mSyncSessionClientImpl.userProfileRequest(mOktaState.getProviderConfiguration(), mTokenResponse);
        ByteBuffer buffer = request.executeRequest(mHttpClient);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject result = new JSONObject(json);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(ConnectionParameters.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNotNull(result);
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }

    @Test
    public void userProfileRequestOAuth2() throws AuthorizationException, OktaRepository.EncryptionException {
        mExpectedEx.expect(AuthorizationException.class);
        //create sessionclient from oauth2 resource
        OIDCConfig oauth2Config = TestValues
                .getConfigWithUrl(mEndPoint.getUrl() + "/oauth2/default/");

        SyncWebAuthClient syncWebAuthClient = new Okta.SyncWebAuthBuilder()
                .withConfig(oauth2Config)
                .withOktaHttpClient(mHttpClient)
                .withContext(mContext)
                .withStorage(new SharedPreferenceStorage(mContext, "oauth2prefs"))
                .withEncryptionManager(new EncryptionManagerStub())
                .create();

        SyncSessionClientImpl sessionClient = (SyncSessionClientImpl) syncWebAuthClient
                .getSessionClient();

        AuthorizedRequest request = sessionClient.userProfileRequest(mOktaState.getProviderConfiguration(), mOktaState.getTokenResponse());
        request.executeRequest(mHttpClient);
    }

    @Test
    public void userProfileRequestFailure() throws InterruptedException, AuthorizationException, OktaRepository.EncryptionException, JSONException {
        mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        AuthorizedRequest request = mSyncSessionClientImpl.userProfileRequest(mOktaState.getProviderConfiguration(), mTokenResponse);
        ByteBuffer buffer = request.executeRequest(mHttpClient);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject result = new JSONObject(json);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertNull(result);
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
    }

    @Test
    public void revokeTokenRequest() throws AuthorizationException, InterruptedException, OktaRepository.EncryptionException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        RevokeTokenRequest request = mSyncSessionClientImpl.revokeTokenRequest("access_token", mOktaState.getProviderConfiguration());
        boolean status = request.executeRequest(mHttpClient);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
        assertTrue(status);
    }

    @Test
    public void revokeTokenRequestFailure() throws AuthorizationException, InterruptedException, AuthorizationException, OktaRepository.EncryptionException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        RevokeTokenRequest request = mSyncSessionClientImpl.revokeTokenRequest("access_token", mOktaState.getProviderConfiguration());
        boolean status = request.executeRequest(mHttpClient);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertFalse(status);
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
    }

    @Test
    public void introspectToken() throws AuthorizationException, InterruptedException, OktaRepository.EncryptionException {
        mEndPoint.enqueueIntrospectSuccess();
        IntrospectRequest request =
                mSyncSessionClientImpl.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN, mOktaState.getProviderConfiguration());
        IntrospectInfo response = request.executeRequest(mHttpClient);
        assertTrue(response.isActive());
    }

    @Test
    public void introspectTokenFailure() throws AuthorizationException, InterruptedException, OktaRepository.EncryptionException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        IntrospectRequest request
                = mSyncSessionClientImpl.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN, mOktaState.getProviderConfiguration());
        IntrospectInfo response = request.executeRequest(mHttpClient);
        assertNull(response);
    }

    @Test
    public void authorizedRequest() throws InterruptedException, AuthorizationException,
            JSONException, OktaRepository.EncryptionException {
        mOktaState.save(mTokenResponse);
        //use userinfo for generic authorized request
        mEndPoint.enqueueUserInfoSuccess();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mSyncSessionClientImpl.createAuthorizedRequest(uri, properties, null,
                null, ConnectionParameters.RequestMethod.POST, mOktaState.getProviderConfiguration(), mOktaState.getTokenResponse());
        ByteBuffer buffer = request.executeRequest(mHttpClient);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject result = new JSONObject(json);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("state"), is(CUSTOM_STATE));
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(ConnectionParameters.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNotNull(result);
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }

    @Test
    public void authorizedRequestFailure() throws InterruptedException, AuthorizationException, OktaRepository.EncryptionException, JSONException {
        //use userinfo for generic authorized request
        mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mSyncSessionClientImpl.createAuthorizedRequest(uri, properties, null,
                null, ConnectionParameters.RequestMethod.GET, mOktaState.getProviderConfiguration(), mTokenResponse);
        ByteBuffer buffer = request.executeRequest(mHttpClient);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject result = new JSONObject(json);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("state"), is(CUSTOM_STATE));
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(ConnectionParameters.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
    }

    @Test
    public void authorizedRequestPostJsonBody() throws InterruptedException, AuthorizationException,
            JSONException, OktaRepository.EncryptionException, UnsupportedEncodingException {
        mOktaState.save(mTokenResponse);
        //use userinfo for generic authorized request
        mEndPoint.enqueueUserInfoSuccess();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("Accept", ConnectionParameters.JSON_CONTENT_TYPE);
        properties.put(CONTENT_TYPE, JSON_CONTENT_TYPE);

        byte[] samplePostBody = JsonStrings.PROVIDER_CONFIG.getBytes(DEFAULT_ENCODING);
        AuthorizedRequest request = mSyncSessionClientImpl.createAuthorizedRequest(uri, properties, null,
                samplePostBody,
                ConnectionParameters.RequestMethod.POST, mOktaState.getProviderConfiguration(), mOktaState.getTokenResponse());
        ByteBuffer buffer = request.executeRequest(mHttpClient);
        assertNotNull(buffer);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject result = new JSONObject(json);

        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        final byte[] recordedPostBody = recordedRequest.getBody().buffer().readByteArray();
        assertArrayEquals(samplePostBody, recordedPostBody);

        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(ConnectionParameters.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getMethod(), is(ConnectionParameters.RequestMethod.POST.name()));

        assertNotNull(result);
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }
}
