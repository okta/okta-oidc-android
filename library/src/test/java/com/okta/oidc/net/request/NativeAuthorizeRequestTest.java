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
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.HttpClientFactory;
import com.okta.oidc.util.TestValues;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.EXCHANGE_CODE;
import static com.okta.oidc.util.TestValues.SESSION_TOKEN;
import static com.okta.oidc.util.TestValues.getProviderConfiguration;
import static org.junit.Assert.assertNotNull;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class NativeAuthorizeRequestTest {
    NativeAuthorizeRequest mRequest;
    private MockEndPoint mEndPoint;
    private ProviderConfiguration mProviderConfig;
    private OktaHttpClient mHttpClient;
    private HttpClientFactory mClientFactory;
    private final int mClientType;

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HttpClientFactory.USE_DEFAULT_HTTP},
                {HttpClientFactory.USE_OK_HTTP},
                {HttpClientFactory.USE_SYNC_OK_HTTP}});
    }

    public NativeAuthorizeRequestTest(int clientType) {
        mClientType = clientType;
    }

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        OIDCConfig config = TestValues.getConfigWithUrl(url);
        mProviderConfig = getProviderConfiguration(url);
        mRequest = TestValues.getNativeLogInRequest(config, SESSION_TOKEN, mProviderConfig);
        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);
        mHttpClient = mClientFactory.build();
    }

    @After
    public void tearDown() throws Exception {
        mEndPoint.shutDown();
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueNativeRequestSuccess(CUSTOM_STATE);
        AuthorizeResponse result = mRequest.executeRequest(mHttpClient);
        assertNotNull(result);
        assertNotNull(result.getCode(), EXCHANGE_CODE);
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        mRequest.executeRequest(mHttpClient);
    }
}