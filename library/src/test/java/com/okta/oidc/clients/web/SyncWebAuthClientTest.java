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

package com.okta.oidc.clients.web;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.okta.oidc.AuthenticationResultHandler;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.OktaResultFragment;
import com.okta.oidc.OktaState;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.request.ConfigurationRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.EncryptionManagerStub;
import com.okta.oidc.util.HttpClientFactory;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import okhttp3.mockwebserver.RecordedRequest;

import static android.app.Activity.RESULT_OK;
import static com.okta.oidc.AuthenticationResultHandler.StateResult;
import static com.okta.oidc.AuthenticationResultHandler.handler;
import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.CUSTOM_CODE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.SCOPES;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class SyncWebAuthClientTest {

    private Context mContext;
    private HttpClientFactory mClientFactory;
    private OIDCConfig mConfig;
    private OktaStorage mStorage;
    private SyncWebAuthClientImpl mSyncWebAuth;
    private MockEndPoint mEndPoint;
    private Gson mGson;
    private OktaState mOktaState;
    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
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

    public SyncWebAuthClientTest(int clientType) {
        mClientType = clientType;
    }

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();


        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);
        mHttpClient = mClientFactory.build();
        mConfig = TestValues.getConfigWithUrl(url);
        mStorage = new SharedPreferenceStorage(mContext);
        mGson = new Gson();

        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        SyncWebAuthClient okta = new Okta.SyncWebAuthBuilder()
                .withConfig(mConfig)
                .withOktaHttpClient(mHttpClient)
                .withContext(mContext)
                .withStorage(mStorage)
                .withEncryptionManager(new EncryptionManagerStub())
                .create();


        mSyncWebAuth = (SyncWebAuthClientImpl) okta;

        mOktaState = mSyncWebAuth.getOktaState();
        mOktaState.save(mProviderConfig);
    }

    @Test
    public void configurationRequest() throws AuthorizationException, InterruptedException {
        mEndPoint.enqueueConfigurationSuccess();
        ConfigurationRequest request = mSyncWebAuth.configurationRequest();
        ProviderConfiguration configuration = request.executeRequest(mHttpClient);
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
        ConfigurationRequest request = mSyncWebAuth.configurationRequest();
        ProviderConfiguration configuration = request.executeRequest(mHttpClient);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(),
                equalTo("//.well-known/openid-configuration?client_id=CLIENT_ID"));
        assertNull(configuration);
    }

    @Test
    public void tokenExchangeFailure() throws InterruptedException, JSONException, AuthorizationException, OktaRepository.EncryptionException {
        mExpectedEx.expect(AuthorizationException.class);
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        String nonce = CodeVerifierUtil.generateRandomState();

        AuthorizeRequest request = new AuthorizeRequest.Builder().codeVerifier(codeVerifier)
                .authorizeEndpoint(mProviderConfig.authorization_endpoint)
                .redirectUri(mConfig.getRedirectUri().toString())
                .scope(SCOPES)
                .nonce(nonce)
                .create();

        mOktaState.save(request);

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        mEndPoint.enqueueReturnInvalidClient();
        TokenRequest tokenRequest = mSyncWebAuth.tokenExchange(response, mOktaState.getProviderConfiguration(), (AuthorizeRequest) mOktaState.getAuthorizeRequest());
        TokenResponse tokenResponse = tokenRequest.executeRequest(mHttpClient);
        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/token"));
        assertNull(tokenResponse);
    }

    @Test
    public void tokenExchangeSuccess() throws InterruptedException, JSONException, AuthorizationException, OktaRepository.EncryptionException {
        String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();
        String nonce = CodeVerifierUtil.generateRandomState();

        AuthorizeRequest request = new AuthorizeRequest.Builder().codeVerifier(codeVerifier)
                .authorizeEndpoint(mProviderConfig.authorization_endpoint)
                .redirectUri(mConfig.getRedirectUri().toString())
                .scope("openid", "email", "profile")
                .nonce(nonce)
                .create();

        mOktaState.save(request);

        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse("com.okta.test:/callback?code=CODE&state=CUSTOM_STATE"));

        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mConfig.getClientId());

        mEndPoint.enqueueTokenSuccess(jws);
        TokenRequest tokenRequest = mSyncWebAuth.tokenExchange(response, mOktaState.getProviderConfiguration(), (AuthorizeRequest) mOktaState.getAuthorizeRequest());
        TokenResponse tokenResponse = tokenRequest.executeRequest(mHttpClient);

        RecordedRequest recordedRequest = mEndPoint.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/token"));
        assertNotNull(tokenResponse);
        assertEquals(tokenResponse.getIdToken(), jws);
    }

    @Test
    public void handleActivityResult() throws InterruptedException {
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/callback?code=" + CUSTOM_CODE + "&state=" + CUSTOM_STATE));
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            latch.countDown();
        });
        handler().onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        AuthorizeResponse response = (AuthorizeResponse) stateResult[0].getAuthorizationResponse();
        assertNotNull(response);
        assertEquals(stateResult[0].getStatus(), AuthenticationResultHandler.Status.AUTHORIZED);
        assertEquals(response.getState(), CUSTOM_STATE);
        assertEquals(response.getCode(), CUSTOM_CODE);
    }

    @Test
    public void signOutWithNoData() {
        mSyncWebAuth.getSessionClient().clear();
        Result result = mSyncWebAuth.signOutOfOkta(Robolectric.setupActivity(FragmentActivity.class));
        assertNotNull(result.getError());
        assertTrue(result.getError().getCause() instanceof NullPointerException);
    }

    @Test
    public void signInEmailAuthenticated() throws AuthorizationException {
        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse(String.format(TestValues.EMAIL_AUTHENTICATED, mEndPoint.getUrl())));

        assertTrue(mSyncWebAuth.isVerificationFlow(response));
        Result result = mSyncWebAuth.processEmailVerification(response);
        assertEquals(AuthorizationStatus.EMAIL_VERIFICATION_AUTHENTICATED, result.getStatus());
        assertNull(result.getLoginHint());
    }

    @Test
    public void signInEmailUnauthenticated() throws AuthorizationException {
        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse(String.format(TestValues.EMAIL_UNAUTHENTICATED, mEndPoint.getUrl())));

        assertTrue(mSyncWebAuth.isVerificationFlow(response));
        Result result = mSyncWebAuth.processEmailVerification(response);
        assertEquals(AuthorizationStatus.EMAIL_VERIFICATION_UNAUTHENTICATED, result.getStatus());
        assertEquals(TestValues.LOGIN_HINT, result.getLoginHint());
    }

    @Test
    public void invalidTyeHint() throws AuthorizationException {
        AuthorizeResponse response = AuthorizeResponse.
                fromUri(Uri.parse(String.format(TestValues.EMAIL_INVALID_TYPE, mEndPoint.getUrl())));

        assertFalse(mSyncWebAuth.isVerificationFlow(response));
    }
}
