package com.okta.oidc;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.json.JSONObject;
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

import static android.content.Context.MODE_PRIVATE;
import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.SCOPES;
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
public class SyncAuthenticationClientTest {
    private Context mContext;
    private SyncAuthenticationClient mSyncAuthClient;
    private HttpConnectionFactory mConnectionFactory;
    private OIDCAccount mAccount;
    private MockEndPoint mEndPoint;
    private OktaStorage mStorage;
    private Gson mGson;
    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();
        mStorage = new SimpleOktaStorage(mContext.getSharedPreferences("OktaTest",
                MODE_PRIVATE));
        mGson = new Gson();
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);
        mSyncAuthClient = new SyncAuthenticationClient(mConnectionFactory, mAccount,
                0, mStorage, mContext, null);
        mSyncAuthClient.mOktaState.save(mProviderConfig);

    }

    @Test
    public void clear_success() {
        mSyncAuthClient.mOktaState.save(mProviderConfig);
        mSyncAuthClient.mOktaState.save(mTokenResponse);
        mSyncAuthClient.mOktaState.save(TestValues.getAuthorizeRequest(mAccount, null));

        mSyncAuthClient.clear();

        assertNull(mSyncAuthClient.mOktaState.getAuthorizeRequest());
        assertNull(mSyncAuthClient.mOktaState.getProviderConfiguration());
        assertNull(mSyncAuthClient.mOktaState.getTokenResponse());
        assertFalse(mSyncAuthClient.isLoggedIn());
        assertNull(mSyncAuthClient.getTokens());
    }

    @Test
    public void isLoggedIn_success() {
        mSyncAuthClient.mOktaState.save(TestValues.getTokenResponse());

        boolean result = mSyncAuthClient.isLoggedIn();

        assertTrue(result);
        assertNotNull(mSyncAuthClient.getTokens());
    }

    @Test
    public void isLoggedIn_false() {
        boolean result = mSyncAuthClient.isLoggedIn();

        assertFalse(result);
        assertNull(mSyncAuthClient.getTokens());
    }

    @Test
    public void refreshTokenRequest() throws InterruptedException, JSONException, AuthorizationException {
        mSyncAuthClient.mOktaState.save(mTokenResponse);
        RefreshTokenRequest request = mSyncAuthClient.refreshTokenRequest();
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
        mSyncAuthClient.mOktaState.save(mTokenResponse);
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        RefreshTokenRequest request = mSyncAuthClient.refreshTokenRequest();
        TokenResponse response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void configurationRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueConfigurationSuccess();
        ConfigurationRequest request = mSyncAuthClient.configurationRequest();
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
        mSyncAuthClient.mOktaState.save(mTokenResponse);
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
        mSyncAuthClient.mOktaState.save(mTokenResponse);
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
    public void introspectToken() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueIntrospectSuccess();
        IntrospectRequest request =
                mSyncAuthClient.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN);
        IntrospectResponse response = request.executeRequest();
        assertTrue(response.active);
    }

    @Test
    public void introspectTokenFailure() throws AuthorizationException, InterruptedException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        IntrospectRequest request
                = mSyncAuthClient.introspectTokenRequest(ACCESS_TOKEN, TokenTypeHint.ACCESS_TOKEN);
        IntrospectResponse response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void authorizedRequest() throws InterruptedException, AuthorizationException,
            JSONException {
        mSyncAuthClient.mOktaState.save(mTokenResponse);
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
        mSyncAuthClient.mOktaState.save(mTokenResponse);
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

        mSyncAuthClient.mOktaState.save(request);

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        mEndPoint.enqueueReturnInvalidClient();
        TokenRequest tokenRequest = mSyncAuthClient.tokenExchange(response);

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

        mSyncAuthClient.mOktaState.save(request);

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mAccount.getClientId());

        mEndPoint.enqueueTokenSuccess(jws);
        TokenRequest tokenRequest = mSyncAuthClient.tokenExchange(response);
        TokenResponse tokenResponse = tokenRequest.executeRequest();

        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/token"));
        assertNotNull(tokenResponse);
        assertEquals(tokenResponse.getIdToken(), jws);
    }
}
