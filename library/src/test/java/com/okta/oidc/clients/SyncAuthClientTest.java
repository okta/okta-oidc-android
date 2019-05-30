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

package com.okta.oidc.clients;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.OktaState;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.NativeAuthorizeRequest;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.results.Result;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.CodeVerifierUtil;
import com.okta.oidc.util.EncryptionManagerStub;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.EXCHANGE_CODE;
import static com.okta.oidc.util.TestValues.SESSION_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class SyncAuthClientTest {

    private MockEndPoint mEndPoint;
    private Context mContext;
    private OIDCConfig mConfig;
    private OktaStorage mStorage;
    private HttpConnectionFactory mConnectionFactory;
    private SyncAuthClientImpl mSyncNativeAuth;
    private AuthClient mAuthClient;
    private ProviderConfiguration mProviderConfig;
    private OktaState mOktaState;
    private ExecutorService mCallbackExecutor;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mEndPoint = new MockEndPoint();

        String url = mEndPoint.getUrl();
        mConfig = TestValues.getConfigWithUrl(url);
        mStorage = new SimpleOktaStorage(mContext);
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();
        mProviderConfig = TestValues.getProviderConfiguration(url);

        SyncAuthClient okta = new Okta.SyncAuthBuilder()
                .withConfig(mConfig)
                .withHttpConnectionFactory(mConnectionFactory)
                .withContext(mContext)
                .withStorage(mStorage)
                .withEncryptionManager(new EncryptionManagerStub())
                .create();

        mSyncNativeAuth = (SyncAuthClientImpl) okta;

        mCallbackExecutor = Executors.newSingleThreadExecutor();
        mAuthClient = new Okta.AuthBuilder()
                .withConfig(mConfig)
                .withHttpConnectionFactory(mConnectionFactory)
                .withContext(mContext)
                .withStorage(mStorage)
                .withCallbackExecutor(mCallbackExecutor)
                .withEncryptionManager(new EncryptionManagerStub())
                .create();

        mOktaState = mSyncNativeAuth.getOktaState();
        mOktaState.save(mProviderConfig);
    }

    @Test
    public void nativeSignInRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueNativeRequestSuccess(CUSTOM_STATE);
        NativeAuthorizeRequest request =
                mSyncNativeAuth.nativeAuthorizeRequest(SESSION_TOKEN, mProviderConfig, null);
        AuthorizeResponse response = request.executeRequest();
        assertNotNull(response);
        assertEquals(response.getCode(), EXCHANGE_CODE);
        assertEquals(response.getState(), CUSTOM_STATE);
    }

    @Test
    public void nativeSignInRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        NativeAuthorizeRequest request =
                mSyncNativeAuth.nativeAuthorizeRequest(SESSION_TOKEN, mProviderConfig,null);
        AuthorizeResponse response = request.executeRequest();
        assertNull(response);
    }

    @Test
    public void signInNative() throws AuthorizationException, OktaRepository.EncryptionException {
        String nonce = CodeVerifierUtil.generateRandomState();
        String state = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mConfig.getClientId());
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .addParameter("nonce", nonce)
                .setState(state)
                .build();

        mEndPoint.enqueueNativeRequestSuccess(state);
        mEndPoint.enqueueTokenSuccess(jws);

        Result result = mSyncNativeAuth.signIn(SESSION_TOKEN, payload);
        assertNotNull(result);
        Tokens tokens = new Tokens(mOktaState.getTokenResponse());
        assertNotNull(tokens);
        assertNotNull(tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        assertNotNull(tokens.getIdToken());
    }

    @Test
    public void signInNativeCancel() throws AuthorizationException, InterruptedException {
        String nonce = CodeVerifierUtil.generateRandomState();
        String state = CodeVerifierUtil.generateRandomState();
        String jws = TestValues.getJwt(mEndPoint.getUrl(), nonce, mConfig.getClientId());
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .addParameter("nonce", nonce)
                .setState(state)
                .build();

        mEndPoint.enqueueNativeRequestSuccess(state, 3);
        mEndPoint.enqueueTokenSuccess(jws);

        CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<Result, AuthorizationException> mockCallback
                = new MockRequestCallback<>(latch);

        mAuthClient.signIn(SESSION_TOKEN, payload, mockCallback);
        Thread.sleep(200); //wait for request to be created
        mAuthClient.cancel();
        latch.await();

        assertNull(mockCallback.getResult());
        assertNotNull(mockCallback.getException());
        assertEquals(mockCallback.getException().errorDescription, "Canceled");
    }
}
