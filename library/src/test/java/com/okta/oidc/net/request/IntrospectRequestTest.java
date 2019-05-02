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
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectInfo;
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

import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.getProviderConfiguration;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class IntrospectRequestTest {
    private IntrospectRequest mRequest;
    private ExecutorService mCallbackExecutor;
    private MockEndPoint mEndPoint;

    private ProviderConfiguration mProviderConfig;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        OIDCConfig config = TestValues.getConfigWithUrl(url);
        mProviderConfig = getProviderConfiguration(url);
        mRequest = TestValues.getIntrospectTokenRequest(config, ACCESS_TOKEN,
                TokenTypeHint.ACCESS_TOKEN, mProviderConfig);
        mCallbackExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        mCallbackExecutor.shutdown();
        mEndPoint.shutDown();
    }

    @Test
    public void dispatchRequestSuccess() throws InterruptedException {
        mEndPoint.enqueueIntrospectSuccess();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<IntrospectInfo, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        assertTrue(cb.getResult().isActive());
    }

    @Test
    public void dispatchRequestFailure() throws InterruptedException, AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<IntrospectInfo, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        throw cb.getException();
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueIntrospectSuccess();
        IntrospectInfo result = mRequest.executeRequest();
        assertTrue(result.isActive());
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        mRequest.executeRequest();
    }
}