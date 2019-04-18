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

import com.google.gson.Gson;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.JsonStrings;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ConfigurationRequestTest {
    private ConfigurationRequest mRequest;
    private ExecutorService mCallbackExecutor;
    private MockEndPoint mEndPoint;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        OIDCAccount mAccount = TestValues.getAccountWithUrl(url);
        mRequest = (ConfigurationRequest) HttpRequestBuilder.newRequest()
                .request(HttpRequest.Type.CONFIGURATION)
                .account(mAccount)
                .createRequest();
        mCallbackExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        mCallbackExecutor.shutdown();
        mEndPoint.shutDown();
    }

    @Test
    public void dispatchRequestSuccess() throws InterruptedException {
        mEndPoint.enqueueConfigurationSuccess();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<ProviderConfiguration, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();

        ProviderConfiguration other = new Gson().
                fromJson(JsonStrings.PROVIDER_CONFIG, ProviderConfiguration.class);
        ProviderConfiguration configuration = cb.getResult();
        configuration.validate();
        assertNotNull(configuration);
        assertEquals(configuration.persist(), other.persist());
    }

    @Test
    public void dispatchRequestFailure() throws InterruptedException, AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Network error");
        mEndPoint.enqueueConfigurationFailure();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<ProviderConfiguration, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        throw cb.getException();
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueConfigurationSuccess();
        ProviderConfiguration configuration = mRequest.executeRequest();
        ProviderConfiguration other = new Gson().
                fromJson(JsonStrings.PROVIDER_CONFIG, ProviderConfiguration.class);
        assertNotNull(configuration);
        configuration.validate();
        assertEquals(configuration.persist(), other.persist());
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Network error");
        mEndPoint.enqueueConfigurationFailure();
        mRequest.executeRequest();
    }
}