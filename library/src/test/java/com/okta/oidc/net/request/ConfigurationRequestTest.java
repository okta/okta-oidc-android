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
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.JsonStrings;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.okta.oidc.util.TestValues.WELL_KNOWN_OAUTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class ConfigurationRequestTest {
    private ConfigurationRequest mRequest;
    private ConfigurationRequest mRequestOAuth2;
    private ExecutorService mCallbackExecutor;
    private MockEndPoint mEndPoint;
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

    public ConfigurationRequestTest(int clientType) {
        mClientType = clientType;
    }

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        OIDCConfig config = TestValues.getConfigWithUrl(url);
        mRequest = HttpRequestBuilder.newConfigurationRequest()
                .config(config)
                .createRequest();
        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);
        mHttpClient = mClientFactory.build();
        OIDCConfig configOAuth2 =
                TestValues.getConfigWithUrl(url + "/oauth2/default/" + WELL_KNOWN_OAUTH);
        mRequestOAuth2 = HttpRequestBuilder.newConfigurationRequest()
                .config(configOAuth2)
                .createRequest();
        mCallbackExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        mCallbackExecutor.shutdown();
        mEndPoint.shutDown();
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueConfigurationSuccess();
        ProviderConfiguration configuration = mRequest.executeRequest(mHttpClient);
        ProviderConfiguration other = new Gson().
                fromJson(JsonStrings.PROVIDER_CONFIG, ProviderConfiguration.class);
        assertNotNull(configuration);
        configuration.validate(false);
        assertEquals(configuration.persist(), other.persist());

        //oauth2
        mEndPoint.enqueueOAuth2ConfigurationSuccess();
        ProviderConfiguration oauth2Result = mRequestOAuth2.executeRequest(mHttpClient);
        ProviderConfiguration oauth2Config = new Gson().
                fromJson(JsonStrings.PROVIDER_CONFIG_OAUTH2, ProviderConfiguration.class);
        assertNotNull(oauth2Result);
        oauth2Config.validate(true);
        assertEquals(oauth2Result.persist(), oauth2Config.persist());
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Invalid status code 404 Client Error");
        mEndPoint.enqueueConfigurationFailure();
        mRequest.executeRequest(mHttpClient);
    }
}