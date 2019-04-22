package com.okta.oidc;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.deprecated.SyncAuthenticationClient;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.RefreshTokenRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.IntrospectResponse;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.storage.OktaRepository;
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

import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.EXCHANGE_CODE;
import static com.okta.oidc.util.TestValues.SCOPES;
import static com.okta.oidc.util.TestValues.SESSION_TOKEN;
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
public class NativeSyncAuthenticationClientTest {
    private Context mContext;
    private SyncAuthenticationClient mSyncAuthClient;
    private HttpConnectionFactory mConnectionFactory;
    private OIDCAccount mAccount;
    private MockEndPoint mEndPoint;
    private OktaStorage mStorage;
    private Gson mGson;
    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
    private OktaState mOktaState;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();
        mStorage = new SimpleOktaStorage(mContext);
        mGson = new Gson();
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        mOktaState = new OktaState(new OktaRepository(mStorage, mContext));

        mOktaState.save(mProviderConfig);
    }




}
