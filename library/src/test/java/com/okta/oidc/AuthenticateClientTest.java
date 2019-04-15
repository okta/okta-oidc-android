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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.test.platform.app.InstrumentationRegistry;
import okhttp3.mockwebserver.RecordedRequest;

import static com.okta.oidc.util.AuthorizationException.TYPE_GENERAL_ERROR;
import static com.okta.oidc.util.AuthorizationException.TYPE_OAUTH_TOKEN_ERROR;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
        mStorage = new SimpleOktaStorage(mContext);
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
        assertEquals(otherClient, client);
    }

    @Test
    public void refreshToken() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        String nonce = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mAccount.getClientId());
        mEndPoint.enqueueTokenSuccess(jws);
        MockRequestCallback<Tokens, AuthorizationException> cb = new MockRequestCallback<>(latch);
        mAuthClient.refreshToken(cb);
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
        mAuthClient.refreshToken(cb);
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
        assertEquals(cb.getException().getMessage(), "Network error");
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
    public void introspectToken() throws InterruptedException {
        mEndPoint.enqueueIntrospectSuccess();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<IntrospectResponse, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mAuthClient.introspectToken(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN, cb);
        latch.await();
        assertNotNull(cb.getResult());
        assertTrue(cb.getResult().active);
    }

    @Test
    public void introspectTokenFailure() throws InterruptedException {
        mEndPoint.enqueueReturnInvalidClient();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<IntrospectResponse, AuthorizationException>
                cb = new MockRequestCallback<>(latch);
        mAuthClient.introspectToken(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN, cb);
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
    }

    private Map<String, String> toMap(RecordedRequest request) {
        final Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return mGson.fromJson(request.getBody().readUtf8(), mapType);
    }
}