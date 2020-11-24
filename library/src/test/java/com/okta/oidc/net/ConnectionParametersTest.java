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

import com.okta.oidc.net.params.GrantTypes;
import com.okta.oidc.net.params.RequestType;
import com.okta.oidc.util.DateUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static com.okta.oidc.net.ConnectionParameters.*;
import static com.okta.oidc.net.ConnectionParameters.USER_AGENT;
import static com.okta.oidc.net.ConnectionParameters.USER_AGENT_HEADER;
import static com.okta.oidc.util.TestValues.CLIENT_ID;
import static com.okta.oidc.util.TestValues.CUSTOM_USER_AGENT;
import static com.okta.oidc.util.TestValues.REDIRECT_URI;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ConnectionParametersTest {
    private ConnectionParameters mConnParams;
    @Rule
    public final MockWebServer mServer = new MockWebServer();
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mConnParams = new ParameterBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setPostParameter("post1", "post1")
                .setRequestProperty("prop1", "prop1")
                .setPostParameters(Collections.singletonMap("post2", "post2"))
                .setRequestProperties(Collections.singletonMap("prop2", "prop2"))
                .create();
    }

    @Test
    public void requestMethod() {
        assertEquals(mConnParams.requestMethod(), RequestMethod.POST);
    }

    @Test
    public void requestProperties() {
        Map<String, String> prop = mConnParams.requestProperties();
        assertTrue(prop.containsKey("prop1"));
        assertTrue(prop.containsKey("prop2"));
    }

    @Test
    public void postParameters() {
        Map<String, String> post = mConnParams.postParameters();
        assertTrue(post.containsKey("post1"));
        assertTrue(post.containsKey("post2"));
    }

    @Test
    public void getEncodedPostParameters() {
        assertNotNull(mConnParams.getEncodedPostParameters());
    }

    @Test
    public void create() throws UnsupportedEncodingException {
        ConnectionParameters parameters = new ParameterBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setRequestType(RequestType.AUTHORIZE)
                .setPostParameter("post1", "post1")
                .setRequestProperty("prop1", "prop1")
                .setPostParameters(Collections.singletonMap("post2", "post2"))
                .setRequestProperties(Collections.singletonMap("prop2", "prop2"))
                .create();

        mConnParams = new ParameterBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setRequestType(RequestType.AUTHORIZE)
                .setPostParameter("post1", "post1")
                .setRequestProperty("prop1", "prop1")
                .setPostParameters(Collections.singletonMap("post2", "post2"))
                .setRequestProperties(Collections.singletonMap("prop2", "prop2"))
                .create();

        String decode1 = new String(parameters.getEncodedPostParameters(), DEFAULT_ENCODING);
        String decode2 = new String(mConnParams.getEncodedPostParameters(), DEFAULT_ENCODING);
        assertEquals(decode1, decode2);
        assertEquals(parameters.getRequestType(), mConnParams.getRequestType());
        assertEquals(parameters.postParameters(), mConnParams.postParameters());
        assertEquals(parameters.requestProperties(), mConnParams.requestProperties());
        assertEquals(parameters.requestMethod(), mConnParams.requestMethod());
    }

    @Test
    public void testBuilderWithRequestProperty() throws Exception {
        mServer.enqueue(new MockResponse());
        Map<String, String> properties = new HashMap<>();
        properties.put(USER_AGENT, CUSTOM_USER_AGENT);
        properties.put("PROP", "PROP");
        URL url = mServer.url("/").url();
        ConnectionParameters parameters = new ParameterBuilder()
                .setRequestMethod(RequestMethod.GET)
                .setRequestProperties(properties)
                .setRequestProperty("PROP1", "PROP1")
                .create();
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.connect(Uri.parse(url.toString()), parameters);

        int code = httpClient.getResponseCode();
        RecordedRequest recordedRequest = mServer.takeRequest();
        assertEquals(code, HTTP_OK);
        //ConnectionParameters overrides custom user agent.
        assertEquals(recordedRequest.getHeader(USER_AGENT), USER_AGENT_HEADER);
        assertEquals(recordedRequest.getHeader("PROP"), "PROP");
        assertEquals(recordedRequest.getHeader("PROP1"), "PROP1");
    }

    @Test
    public void testBuilderWithPostParameters() throws Exception {
        mServer.enqueue(new MockResponse());
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("grant_type", GrantTypes.AUTHORIZATION_CODE);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("code_verifier", "code_verifier");
        params.put("code", "code");
        params.put("nonce", "nonce");
        URL url = mServer.url("/").url();
        ConnectionParameters parameters = new ParameterBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setPostParameters(params)
                .setPostParameter("POST", "POST")
                .create();
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.connect(Uri.parse(url.toString()), parameters);
        int code = httpClient.getResponseCode();
        RecordedRequest recordedRequest = mServer.takeRequest();
        String postbody = recordedRequest.getBody().readUtf8Line();
        assertEquals(code, HTTP_OK);
        Assert.assertTrue(postbody.contains("client_id=" + CLIENT_ID));
        Assert.assertTrue(postbody.contains("grant_type=" + GrantTypes.AUTHORIZATION_CODE));
        Assert.assertTrue(postbody.contains("redirect_uri=" +
                URLEncoder.encode(REDIRECT_URI, "UTF-8")));
        Assert.assertTrue(postbody.contains("code_verifier=code_verifier"));
        Assert.assertTrue(postbody.contains("code=code"));
        Assert.assertTrue(postbody.contains("nonce=nonce"));
    }

    @Test
    public void testBuilderWithTimeOut() throws Exception {
        mServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        URL url = mServer.url("/").url();
        Date start = DateUtil.getNow();
        ConnectionParameters parameters = new ParameterBuilder()
                .setRequestMethod(RequestMethod.GET)
                .create();
        HttpClientImpl httpClient = new HttpClientImpl(5000, 2000);
        httpClient.connect(Uri.parse(url.toString()), parameters);

        try {
            httpClient.getResponseCode();
        } catch (SocketTimeoutException ex) {
            //NO-OP
        }
        long end = DateUtil.getNow().getTime();
        Date date = new Date(end - 2000);
        assertEquals(start.toString(), date.toString());
    }
}
