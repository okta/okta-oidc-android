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
package com.okta.oidc.net.request;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;
import com.okta.oidc.util.TestValues;

import org.junit.After;
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

import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.CUSTOM_NONCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class RefreshTokenRequestTest {
    private RefreshTokenRequest mRequest;
    private OIDCConfig mAccount;
    private ExecutorService mCallbackExecutor;
    private MockEndPoint mEndPoint;
    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mRequest = TestValues.getRefreshRequest(mAccount, mTokenResponse, mProviderConfig);
        mCallbackExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        mCallbackExecutor.shutdown();
        mEndPoint.shutDown();
    }

    @Test
    public void dispatchRequestSuccess() throws AuthorizationException, InterruptedException {
        String jws = TestValues.getJwt(mEndPoint.getUrl(), CUSTOM_NONCE, mAccount.getClientId());
        mEndPoint.enqueueTokenSuccess(jws);
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<TokenResponse, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        TokenResponse response = cb.getResult();
        assertNotNull(response);
        assertEquals(response.getIdToken(), jws);
    }

    @Test
    public void dispatchRequestFailure() throws AuthorizationException, InterruptedException {
        String jws = TestValues.getJwt(mEndPoint.getUrl(), CUSTOM_NONCE, mAccount.getClientId());
        mEndPoint.enqueueReturnInvalidClient();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<TokenResponse, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        assertNull(cb.getResult());
        assertNotNull(cb.getException());
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException {
        String jws = TestValues.getJwt(mEndPoint.getUrl(), CUSTOM_NONCE, mAccount.getClientId());
        mEndPoint.enqueueTokenSuccess(jws);
        TokenResponse response = mRequest.executeRequest();
        assertNotNull(response);
        assertEquals(response.getIdToken(), jws);
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnInvalidClient();
        TokenResponse response = mRequest.executeRequest();
        assertNull(response);
    }
}
