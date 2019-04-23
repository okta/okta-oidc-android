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

import com.okta.oidc.net.params.GrantTypes;
import com.okta.oidc.util.DateUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static com.okta.oidc.net.HttpConnection.USER_AGENT;
import static com.okta.oidc.net.HttpConnection.USER_AGENT_HEADER;
import static com.okta.oidc.util.TestValues.CLIENT_ID;
import static com.okta.oidc.util.TestValues.CUSTOM_USER_AGENT;
import static com.okta.oidc.util.TestValues.REDIRECT_URI;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HttpConnectionTest {
    @Rule
    public final MockWebServer mServer = new MockWebServer();

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Test
    public void testBuilderWithConnectionFactory() throws IOException, InterruptedException {
        mServer.enqueue(new MockResponse());
        HttpConnectionFactory customFactory = url -> {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty(USER_AGENT, CUSTOM_USER_AGENT);
            return connection;
        };

        URL url = mServer.url("/").url();
        HttpURLConnection urlConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .create(customFactory)
                .openConnection(url);
        int code = urlConnection.getResponseCode();
        RecordedRequest recordedRequest = mServer.takeRequest();
        assertEquals(code, HTTP_OK);
        //HttpConnection overrides any custom user-agents
        assertEquals(recordedRequest.getHeader(USER_AGENT), USER_AGENT_HEADER);
    }

    @Test
    public void testBuilderWithRequestProperty() throws IOException, InterruptedException {
        mServer.enqueue(new MockResponse());
        Map<String, String> properties = new HashMap<>();
        properties.put(USER_AGENT, CUSTOM_USER_AGENT);
        properties.put("PROP", "PROP");
        URL url = mServer.url("/").url();
        HttpURLConnection urlConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .setRequestProperties(properties)
                .setRequestProperty("PROP1", "PROP1")
                .create()
                .openConnection(url);
        int code = urlConnection.getResponseCode();
        RecordedRequest recordedRequest = mServer.takeRequest();
        assertEquals(code, HTTP_OK);
        //HttpConnection overrides custom user agent.
        assertEquals(recordedRequest.getHeader(USER_AGENT), USER_AGENT_HEADER);
        assertEquals(recordedRequest.getHeader("PROP"), "PROP");
        assertEquals(recordedRequest.getHeader("PROP1"), "PROP1");
    }

    @Test
    public void testBuilderWithPostParameters() throws IOException, InterruptedException {
        mServer.enqueue(new MockResponse());
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("grant_type", GrantTypes.AUTHORIZATION_CODE);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("code_verifier", "code_verifier");
        params.put("code", "code");
        params.put("nonce", "nonce");
        URL url = mServer.url("/").url();
        HttpURLConnection urlConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.POST)
                .setPostParameters(params)
                .setPostParameter("POST", "POST")
                .create()
                .openConnection(url);
        int code = urlConnection.getResponseCode();
        RecordedRequest recordedRequest = mServer.takeRequest();
        String postbody = recordedRequest.getBody().readUtf8Line();
        assertEquals(code, HTTP_OK);
        assertTrue(postbody.contains("client_id=" + CLIENT_ID));
        assertTrue(postbody.contains("grant_type=" + GrantTypes.AUTHORIZATION_CODE));
        assertTrue(postbody.contains("redirect_uri=" +
                URLEncoder.encode(REDIRECT_URI, "UTF-8")));
        assertTrue(postbody.contains("code_verifier=code_verifier"));
        assertTrue(postbody.contains("code=code"));
        assertTrue(postbody.contains("nonce=nonce"));
    }

    @Test
    public void testBuilderWithTimeOut() throws IOException {
        mServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        URL url = mServer.url("/").url();
        Date start = DateUtil.getNow();
        HttpURLConnection urlConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .setReadTimeOutMs(2000)
                .create()
                .openConnection(url);
        try {
            urlConnection.getResponseCode();
        } catch (SocketTimeoutException ex) {
            //NO-OP
        }
        long end = DateUtil.getNow().getTime();
        Date date = new Date(end - 2000);
        assertEquals(start.toString(), date.toString());
    }
}