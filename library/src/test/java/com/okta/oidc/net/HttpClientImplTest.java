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

package com.okta.oidc.net;

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.HttpRequestBuilder;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HttpClientImplTest {
    private AuthorizedRequest mRequest;
    private MockEndPoint mEndPoint;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();
    private HttpClientImpl mHttpClient;

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();

        String url = mEndPoint.getUrl();
        OIDCConfig config = TestValues.getConfigWithUrl(url);
        ProviderConfiguration providerConfiguration = TestValues.getProviderConfiguration(url);
        TokenResponse tokenResponse = new Gson().fromJson(JsonStrings.TOKEN_RESPONSE, TokenResponse.class);
        mHttpClient = new HttpClientImpl();
        mRequest = HttpRequestBuilder.newAuthorizedRequest()
                .uri(Uri.parse(mEndPoint.getUrl()))
                .httpRequestMethod(ConnectionParameters.RequestMethod.POST)
                .config(config)
                .providerConfiguration(providerConfiguration)
                .tokenResponse(tokenResponse)
                .createRequest();
    }

    @Test
    public void connect() throws Exception {
        mEndPoint.enqueueUserInfoSuccess();
        InputStream stream = mHttpClient.connect(Uri.parse(mEndPoint.getUrl()), mRequest.mConnParams);
        HttpResponse response = new HttpResponse(
                mHttpClient.getResponseCode(), mHttpClient.getHeaderFields(),
                mHttpClient.getContentLength(), stream, mHttpClient);
        JSONObject result = response.asJson();
        assertEquals("John Doe", result.get("name"));
        assertEquals("Jimmy", result.get("nickname"));
    }

    @Test
    public void cleanUp() {
        mHttpClient.cleanUp();
        assertNull(mHttpClient.getUrlConnection());
    }

    @Test
    public void cancel() throws AuthorizationException {
        mEndPoint.enqueueUserInfoSuccess(5);
        final CountDownLatch latch = new CountDownLatch(1);
        final ByteBuffer[] result = new ByteBuffer[1];
        final AuthorizationException[] exception = new AuthorizationException[1];
        Thread t = new Thread(() -> {
            try {
                result[0] = mRequest.executeRequest(mHttpClient);
            } catch (AuthorizationException e) {
                exception[0] = e;
            }
        });
        t.start();
        mHttpClient.cancel();
        if (exception[0] != null) {
            //The exception can be canceled or stream is closed.
            String errorMessage = exception[0].getMessage();
            assertTrue("Canceled".equals(errorMessage) || "stream is closed".equals(errorMessage));
        } else {
            assertNull(result[0]);
        }


    }

    @Test
    public void getHeaderFields() throws Exception {
        mEndPoint.enqueueUserInfoSuccess();
        mHttpClient.connect(Uri.parse(mEndPoint.getUrl()), mRequest.mConnParams);
        assertNotNull(mHttpClient.getHeaderFields());
    }

    @Test
    public void getHeader() throws Exception {
        mEndPoint.enqueueUserInfoSuccess();
        mHttpClient.connect(Uri.parse(mEndPoint.getUrl()), mRequest.mConnParams);
        assertNotNull(mHttpClient.getHeader("Content-Length"));
    }

    @Test
    public void getResponseCode() throws Exception {
        mEndPoint.enqueueUserInfoSuccess();
        mHttpClient.connect(Uri.parse(mEndPoint.getUrl()), mRequest.mConnParams);
        assertEquals(HTTP_OK, mHttpClient.getResponseCode());
    }

    @Test
    public void getContentLength() throws Exception {
        mEndPoint.enqueueUserInfoSuccess();
        mHttpClient.connect(Uri.parse(mEndPoint.getUrl()), mRequest.mConnParams);

        assertTrue(mHttpClient.getContentLength() > 1);
    }

    @Test
    public void getResponseMessage() throws Exception {
        mEndPoint.enqueueUserInfoSuccess();
        mHttpClient.connect(Uri.parse(mEndPoint.getUrl()), mRequest.mConnParams);
        assertEquals("OK", mHttpClient.getResponseMessage());
    }
}