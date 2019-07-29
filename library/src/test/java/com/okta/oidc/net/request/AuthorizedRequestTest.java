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

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.HttpClientFactory;
import com.okta.oidc.util.OkHttp;
import com.okta.oidc.util.TestValues;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import static com.okta.oidc.net.ConnectionParameters.DEFAULT_ENCODING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthorizedRequestTest {
    private AuthorizedRequest mRequest;
    private MockEndPoint mEndPoint;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private ProviderConfiguration mProviderConfig;
    private TokenResponse mTokenResponse;
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

    public AuthorizedRequestTest(Integer clientType) {
        mClientType = clientType;
    }

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        OIDCConfig config = TestValues.getConfigWithUrl(url);
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mTokenResponse = new Gson().fromJson(JsonStrings.TOKEN_RESPONSE, TokenResponse.class);
        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);
        mHttpClient = mClientFactory.build();
        mRequest = HttpRequestBuilder.newAuthorizedRequest()
                .uri(Uri.parse(mEndPoint.getUrl()))
                .httpRequestMethod(ConnectionParameters.RequestMethod.POST)
                .config(config)
                .providerConfiguration(mProviderConfig)
                .tokenResponse(mTokenResponse)
                .createRequest();
    }

    @After
    public void tearDown() throws Exception {
        mEndPoint.shutDown();
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException, JSONException {
        mEndPoint.enqueueUserInfoSuccess();
        ByteBuffer buffer = mRequest.executeRequest(mHttpClient);
        assertNotNull(buffer);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject jsonObject = new JSONObject(json);
        assertNotNull(jsonObject);
        assertEquals(jsonObject.getString("nickname"), "Jimmy");
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Invalid status code 401 Client Error");
        mEndPoint.enqueueReturnInvalidClient();
        mRequest.executeRequest(mHttpClient);
    }

    @Test
    public void executeRequestSuccessWithOkHttp() throws AuthorizationException, JSONException {
        mEndPoint.enqueueUserInfoSuccess();
        ByteBuffer buffer = mRequest.executeRequest(new OkHttp());
        assertNotNull(buffer);
        String json = Charset.forName(DEFAULT_ENCODING).decode(buffer).toString();
        JSONObject jsonObject = new JSONObject(json);
        assertNotNull(jsonObject);
        assertEquals(jsonObject.getString("nickname"), "Jimmy");
    }

    @Test
    public void executeRequestFailureWithOkHttp() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mExpectedEx.expectMessage("Invalid status code 401 Client Error");
        mEndPoint.enqueueReturnInvalidClient();
        mRequest.executeRequest(new OkHttp());
    }
}