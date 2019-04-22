package com.okta.oidc.clients.natives;

import android.content.Context;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.Okta;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.sessions.SyncSession;
import com.okta.oidc.sessions.SyncSessionClientFactory;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import androidx.test.platform.app.InstrumentationRegistry;

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.EXCHANGE_CODE;
import static com.okta.oidc.util.TestValues.SESSION_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SyncNativeAuthClientTest {

    private MockEndPoint mEndPoint;
    private Context mContext;
    private OIDCAccount mAccount;
    private OktaStorage mStorage;
    private HttpConnectionFactory mConnectionFactory;
    private SyncNativeAuthClient mSyncNativeAuth;
    private ProviderConfiguration mProviderConfig;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mEndPoint = new MockEndPoint();

        String url = mEndPoint.getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
        mStorage = new SimpleOktaStorage(mContext);
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();
        mProviderConfig = TestValues.getProviderConfiguration(url);

        Okta<SyncNativeAuth, SyncSession> okta = new Okta.Builder<SyncNativeAuth, SyncSession>()
                .withAccount(mAccount)
                .withHttpConnectionFactory(mConnectionFactory)
                .withContext(mContext)
                .withStorage(mStorage)
                .withAuthenticationClientFactory(new SyncNativeAuthClientFactory())
                .withSessionClientFactory(new SyncSessionClientFactory())
                .create();

        mSyncNativeAuth = (SyncNativeAuthClient)okta.getAuthorizationClient();
    }

    @Test
    public void nativeLogInRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueNativeRequestSuccess(CUSTOM_STATE);
        NativeAuthorizeRequest request =
                mSyncNativeAuth.nativeAuthorizeRequest(SESSION_TOKEN, null);
        AuthorizeResponse response = request.executeRequest();
        assertNotNull(response);
        assertEquals(response.getCode(), EXCHANGE_CODE);
        assertEquals(response.getState(), CUSTOM_STATE);
    }

    @Test
    public void nativeLogInRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        NativeAuthorizeRequest request =
                mSyncNativeAuth.nativeAuthorizeRequest(SESSION_TOKEN, null);
        AuthorizeResponse response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void loginNative() throws AuthorizationException {
        String nonce = CodeVerifierUtil.generateRandomState();
        String state = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mAccount.getClientId());
        AuthenticationPayload payload =
                new AuthenticationPayload.Builder().addParameter("nonce", nonce)
                        .setState(state).build();

        mEndPoint.enqueueConfigurationSuccess(mProviderConfig);
        mEndPoint.enqueueNativeRequestSuccess(state);
        mEndPoint.enqueueTokenSuccess(jws);

        AuthorizationResult result = mSyncNativeAuth.logIn(SESSION_TOKEN, payload);
        assertNotNull(result);
        Tokens tokens = result.getTokens();
        assertNotNull(tokens);
        assertNotNull(tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        assertNotNull(tokens.getIdToken());
    }
}
