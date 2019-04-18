package com.okta.oidc.clients.sessions;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.SyncWebAuth;
import com.okta.oidc.clients.SyncWebAuthClientFactory;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectResponse;
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

import androidx.test.platform.app.InstrumentationRegistry;
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
public class SyncSessionClientTest {

    private Context mContext;
    private OIDCConfig mAccount;
    private HttpConnectionFactory mConnectionFactory;
    private OktaStorage mStorage;
    ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
    private SyncWebAuth mSyncWebAuth;
    private SyncSessionClient mSyncSessionClient;
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

        SyncWebAuth okta = new Okta.SyncWebBuilder()
                .withConfig(mAccount)
                .withHttpConnectionFactory(mConnectionFactory)
                .withContext(mContext)
                .withStorage(mStorage)
                .create();

        mSyncWebAuth = okta;
        mSyncSessionClient = (SyncSessionClient) okta.getSessionClient();

        mOktaState = mSyncSessionClient.getOktaState();

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

        mSyncSessionClient.clear();

        assertNull(mOktaState.getAuthorizeRequest());
        assertNull(mOktaState.getProviderConfiguration());
        assertNull(mOktaState.getTokenResponse());
        assertFalse(mSyncSessionClient.isLoggedIn());
        assertNull(mSyncSessionClient.getTokens());
    }

    @Test
    public void isLoggedIn_success() {
        mOktaState.save(TestValues.getTokenResponse());

        boolean result = mSyncSessionClient.isLoggedIn();

        assertTrue(result);
        assertNotNull(mSyncSessionClient.getTokens());
    }

    @Test
    public void isLoggedIn_false() {
        boolean result = mSyncSessionClient.isLoggedIn();

        assertFalse(result);
        assertNull(mSyncSessionClient.getTokens());
    }

    @Test
    public void refreshTokenRequest() throws InterruptedException, JSONException, AuthorizationException {
        mOktaState.save(mTokenResponse);
        RefreshTokenRequest request = mSyncSessionClient.refreshTokenRequest();
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
        RefreshTokenRequest request = mSyncSessionClient.refreshTokenRequest();
        TokenResponse response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void userProfileRequest() throws InterruptedException, AuthorizationException,
            JSONException {
        mOktaState.save(mTokenResponse);
        mEndPoint.enqueueUserInfoSuccess();
        AuthorizedRequest request = mSyncSessionClient.userProfileRequest();
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
        AuthorizedRequest request = mSyncSessionClient.userProfileRequest();
        JSONObject result = request.executeRequest();
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertNull(result);
        assertThat(recordedRequest.getPath(), equalTo("/userinfo"));
    }

    @Test
    public void revokeTokenRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        RevokeTokenRequest request = mSyncSessionClient.revokeTokenRequest("access_token");
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
        RevokeTokenRequest request = mSyncSessionClient.revokeTokenRequest("access_token");
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
                mSyncSessionClient.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN);
        IntrospectResponse response = request.executeRequest();
        assertTrue(response.active);
    }

    @Test
    public void introspectTokenFailure() throws AuthorizationException, InterruptedException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        IntrospectRequest request
                = mSyncSessionClient.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN);
        IntrospectResponse response = request.executeRequest();
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
        AuthorizedRequest request = mSyncSessionClient.authorizedRequest(uri, properties,
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
        AuthorizedRequest request = mSyncSessionClient.authorizedRequest(uri, properties,
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
