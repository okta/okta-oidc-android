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
import com.okta.oidc.Tokens;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.EncryptionManagerStub;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.mockwebserver.RecordedRequest;

import static com.okta.oidc.net.ConnectionParameters.CONTENT_TYPE;
import static com.okta.oidc.net.ConnectionParameters.DEFAULT_ENCODING;
import static com.okta.oidc.net.ConnectionParameters.JSON_CONTENT_TYPE;
import static com.okta.oidc.util.AuthorizationException.TYPE_GENERAL_ERROR;
import static com.okta.oidc.util.AuthorizationException.TYPE_OAUTH_TOKEN_ERROR;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class SessionClientImplTest {

    private Context mContext;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private HttpClientFactory mClientFactory;
    private MockEndPoint mEndPoint;

    private OktaStorage mStorage;

    private OIDCConfig mConfig;
    private SessionClient mSessionClient;
    private Gson mGson;

    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();
    private final int mClientType;

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HttpClientFactory.USE_DEFAULT_HTTP},
                {HttpClientFactory.USE_OK_HTTP},
                {HttpClientFactory.USE_SYNC_OK_HTTP}
        });
    }

    public SessionClientImplTest(int clientType) {
        mClientType = clientType;
    }

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mEndPoint = new MockEndPoint();
        mGson = new Gson();
        mStorage = new SharedPreferenceStorage(mContext);
        String url = mEndPoint.getUrl();
        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);

        mConfig = TestValues.getConfigWithUrl(url);
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        WebAuthClient okta = new Okta.WebAuthBuilder()
                .withCallbackExecutor(mExecutor)
                .withConfig(mConfig)
                .withOktaHttpClient(mClientFactory.build())
                .withContext(mContext)
                .withStorage(mStorage)
                .withEncryptionManager(new EncryptionManagerStub())
                .create();

        mSessionClient = okta.getSessionClient();

        OktaState mOktaState = new OktaState(new OktaRepository(mStorage, mContext,
                new EncryptionManagerStub(), false, false));

        mOktaState.save(mTokenResponse);
        mOktaState.save(mProviderConfig);
    }

    @After
    public void tearDown() throws Exception {
        try {
            mEndPoint.shutDown();
            mExecutor.shutdown();
        } catch (IOException io) {
            //NO-OP
        }
    }

    @Test
    public void refreshToken() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        String nonce = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mConfig.getClientId());
        mEndPoint.enqueueTokenSuccess(jws);
        MockRequestCallback<Tokens, AuthorizationException> cb = new MockRequestCallback<>(latch);
        mSessionClient.refreshToken(cb);
        latch.await();
        Tokens result = cb.getResult();
        TokenResponse original = mGson.fromJson(String.format(TOKEN_SUCCESS, jws),
                TokenResponse.class);
        assertEquals(original.getIdToken(), result.getIdToken());
        assertEquals(original.getRefreshToken(), result.getRefreshToken());
        assertEquals(original.getIdToken(), result.getIdToken());
    }

    @Test
    public void refreshTokenFailure() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mEndPoint.enqueueReturnInvalidClient();
        MockRequestCallback<Tokens, AuthorizationException> cb = new MockRequestCallback<>(latch);
        mSessionClient.refreshToken(cb);
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
        assertEquals(cb.getException().getMessage(), "Invalid status code 401 Client Error");
    }

    @Test
    public void getUserProfile() throws InterruptedException, JSONException {
        mEndPoint.enqueueUserInfoSuccess();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<UserInfo, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        mSessionClient.getUserProfile(cb);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        latch.await();
        UserInfo result = cb.getResult();
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(ConnectionParameters.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNotNull(result);
        assertEquals("John Doe", result.get("name"));
        assertEquals("Jimmy", result.get("nickname"));
    }

    @Test
    public void getUserProfileFailure() throws InterruptedException, JSONException {
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<UserInfo, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        mSessionClient.getUserProfile(cb);
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
        mSessionClient.revokeToken("access_token", cb);
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
        mSessionClient.revokeToken("access_token", cb);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
        assertEquals(TYPE_OAUTH_TOKEN_ERROR, cb.getException().type);
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
    }

    @Test
    public void introspectToken() throws InterruptedException {
        mEndPoint.enqueueIntrospectSuccess();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<IntrospectInfo, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mSessionClient.introspectToken(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN, cb);
        latch.await();
        assertNotNull(cb.getResult());
        assertTrue(cb.getResult().isActive());
    }

    @Test
    public void introspectTokenFailure() throws InterruptedException {
        mEndPoint.enqueueReturnInvalidClient();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<IntrospectInfo, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mSessionClient.introspectToken(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN, cb);
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
    }

    @Test
    public void authorizedRequest() throws InterruptedException, JSONException {
        mEndPoint.enqueueUserInfoSuccess();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);

        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<JSONObject, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mSessionClient.authorizedRequest(uri, properties, null, ConnectionParameters.RequestMethod.GET, cb);
        latch.await();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertNotNull(cb.getResult());
        JSONObject result = cb.getResult();
        assertThat(recordedRequest.getHeader("state"), is(CUSTOM_STATE));
        assertNull(cb.getException());
        assertEquals("John Doe", result.getString("name"));
        assertEquals("Jimmy", result.getString("nickname"));
    }

    @Test
    public void authorizedRequestFailure() throws InterruptedException, JSONException {
        mEndPoint.enqueueReturnInvalidClient();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);

        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<JSONObject, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mSessionClient.authorizedRequest(uri, properties, null, ConnectionParameters.RequestMethod.GET, cb);
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
    }

    @Test
    public void authorizedRequestCancel() throws InterruptedException, JSONException {
        mEndPoint.enqueueUserInfoSuccess(5);
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);

        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<JSONObject, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mSessionClient.authorizedRequest(uri, properties, null, ConnectionParameters.RequestMethod.GET, cb);
        Thread.sleep(200); //wait for request to be created
        mSessionClient.cancel();
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
        String errorMessage = cb.getException().getMessage();
        //The errorMessage can be null if its a Interrupt. The errorMessage can be of
        //Socket closed or canceled or stream is closed or network error.
        if (errorMessage == null) {
            assertTrue(cb.getException().getCause() instanceof InterruptedException);
        } else {
            assertTrue("Socket closed".equals(errorMessage) || "Canceled".equals(errorMessage)
                    || "stream is closed".equals(errorMessage) || "Network error".equals(errorMessage)
                    || "interrupted".equals(errorMessage));
        }
    }

    @Test
    public void authorizedRequestPostJsonBody() throws InterruptedException, JSONException, UnsupportedEncodingException {
        mEndPoint.enqueueUserInfoSuccess();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("Accept", ConnectionParameters.JSON_CONTENT_TYPE);
        properties.put(CONTENT_TYPE, JSON_CONTENT_TYPE);

        byte[] samplePostBody = JsonStrings.PROVIDER_CONFIG.getBytes(DEFAULT_ENCODING);
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<ByteBuffer, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mSessionClient.authorizedRequest(uri, properties, null, samplePostBody,
                ConnectionParameters.RequestMethod.POST, cb);
        latch.await();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        final byte[] recordedPostBody = recordedRequest.getBody().buffer().readByteArray();
        assertArrayEquals(samplePostBody, recordedPostBody);
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(ConnectionParameters.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getMethod(), is(ConnectionParameters.RequestMethod.POST.name()));
        assertNotNull(cb.getResult());

        ByteBuffer result = cb.getResult();
        String json = Charset.forName(DEFAULT_ENCODING).decode(result).toString();
        JSONObject jsonObject = new JSONObject(json);

        assertNull(cb.getException());
        assertEquals("John Doe", jsonObject.getString("name"));
        assertEquals("Jimmy", jsonObject.getString("nickname"));
    }
}
