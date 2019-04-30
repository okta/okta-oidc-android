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
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.MockEndPoint;
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

import java.util.HashMap;

import okhttp3.mockwebserver.RecordedRequest;

import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class SyncSessionClientImplImplTest {

    private Context mContext;
    private OIDCConfig mAccount;
    private HttpConnectionFactory mConnectionFactory;
    private OktaStorage mStorage;
    ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
    private SyncWebAuthClient mSyncWebAuth;
    private SyncSessionClientImpl mSyncSessionClientImpl;
    private MockEndPoint mEndPoint;
    private Gson mGson;
    private OktaState mOktaState;


    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
        mGson = new Gson();
        mStorage = new SimpleOktaStorage(mContext);

        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        SyncWebAuthClient okta = new Okta.SyncWebAuthBuilder()
                .withConfig(mAccount)
                .withHttpConnectionFactory(mConnectionFactory)
                .withContext(mContext)
                .withStorage(mStorage)
                .create();

        mSyncWebAuth = okta;
        mSyncSessionClientImpl = (SyncSessionClientImpl) okta.getSessionClient();

        mOktaState = mSyncSessionClientImpl.getOktaState();

        mOktaState.save(mProviderConfig);
    }

    @After
    public void tearDown() throws Exception {
        mEndPoint.shutDown();
    }


    @Test
    public void clear_success() {
        mOktaState.save(mProviderConfig);
        mOktaState.save(mTokenResponse);
        mOktaState.save(TestValues.getAuthorizeRequest(mAccount, null));

        mSyncSessionClientImpl.clear();

        assertNull(mOktaState.getAuthorizeRequest());
        assertNull(mOktaState.getProviderConfiguration());
        assertNull(mOktaState.getTokenResponse());
        assertFalse(mSyncSessionClientImpl.isLoggedIn());
        assertNull(mSyncSessionClientImpl.getTokens());
    }

    @Test
    public void isLoggedIn_success() {
        mOktaState.save(TestValues.getTokenResponse());

        boolean result = mSyncSessionClientImpl.isLoggedIn();

        assertTrue(result);
        assertNotNull(mSyncSessionClientImpl.getTokens());
    }

    @Test
    public void isLoggedIn_false() {
        boolean result = mSyncSessionClientImpl.isLoggedIn();

        assertFalse(result);
        assertNull(mSyncSessionClientImpl.getTokens());
    }

    @Test
    public void refreshTokenRequest() throws InterruptedException, JSONException, AuthorizationException {
        mOktaState.save(mTokenResponse);
        RefreshTokenRequest request = mSyncSessionClientImpl.refreshTokenRequest();
        String nonce = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mAccount.getClientId());
        mEndPoint.enqueueTokenSuccess(jws);
        TokenResponse response = request.executeRequest();
        assertNotNull(response);

        TokenResponse original = mGson.fromJson(String.format(TOKEN_SUCCESS, jws),
                TokenResponse.class);
        assertEquals(original.getIdToken(), response.getIdToken());
        assertEquals(original.getRefreshToken(), response.getRefreshToken());
        assertEquals(original.getIdToken(), response.getIdToken());
    }

    @Test
    public void refreshTokenRequestFailure() throws InterruptedException, JSONException, AuthorizationException {
        mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        RefreshTokenRequest request = mSyncSessionClientImpl.refreshTokenRequest();
        TokenResponse response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void userProfileRequest() throws InterruptedException, AuthorizationException,
            JSONException {
        mOktaState.save(mTokenResponse);
        mEndPoint.enqueueUserInfoSuccess();
        AuthorizedRequest request = mSyncSessionClientImpl.userProfileRequest();
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
        mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        AuthorizedRequest request = mSyncSessionClientImpl.userProfileRequest();
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertNull(result);
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
    }

    @Test
    public void revokeTokenRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        RevokeTokenRequest request = mSyncSessionClientImpl.revokeTokenRequest("access_token");
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
        RevokeTokenRequest request = mSyncSessionClientImpl.revokeTokenRequest("access_token");
        boolean status = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertFalse(status);
        assertThat(recordedRequest.getPath(),
                equalTo("/revoke?client_id=CLIENT_ID&token=access_token"));
    }

    @Test
    public void introspectToken() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueIntrospectSuccess();
        IntrospectRequest request =
                mSyncSessionClientImpl.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN);
        IntrospectInfo response = request.executeRequest();
        assertTrue(response.isActive());
    }

    @Test
    public void introspectTokenFailure() throws AuthorizationException, InterruptedException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        IntrospectRequest request
                = mSyncSessionClientImpl.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN);
        IntrospectInfo response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void authorizedRequest() throws InterruptedException, AuthorizationException,
            JSONException {
        mOktaState.save(mTokenResponse);
        //use userinfo for generic authorized request
        mEndPoint.enqueueUserInfoSuccess();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mSyncSessionClientImpl.authorizedRequest(uri, properties,
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
        mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        Uri uri = Uri.parse(mProviderConfig.userinfo_endpoint);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("state", CUSTOM_STATE);
        AuthorizedRequest request = mSyncSessionClientImpl.authorizedRequest(uri, properties,
                null, HttpConnection.RequestMethod.GET);
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getHeader("state"), is(CUSTOM_STATE));
        assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + ACCESS_TOKEN));
        assertThat(recordedRequest.getHeader("Accept"), is(HttpConnection.JSON_CONTENT_TYPE));
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
        assertNull(result);
    }
}
