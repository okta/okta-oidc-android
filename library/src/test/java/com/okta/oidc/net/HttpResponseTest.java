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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.okta.oidc.net.response.TokenResponse;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.okta.oidc.net.ConnectionParameters.CONTENT_TYPE;
import static com.okta.oidc.net.ConnectionParameters.JSON_CONTENT_TYPE;
import static com.okta.oidc.util.JsonStrings.CONFIGURATION_NOT_FOUND;
import static com.okta.oidc.util.JsonStrings.FORBIDDEN;
import static com.okta.oidc.util.JsonStrings.TOKEN_SUCCESS;
import static com.okta.oidc.util.JsonStrings.WWW_AUTHENTICATE;
import static com.okta.oidc.util.TestValues.SESSION_TOKEN;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HttpResponseTest {
    @Rule
    public final MockWebServer mServer = new MockWebServer();

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Test
    public void textOKResponse() throws Exception {
        mServer.enqueue(new MockResponse().setBody("test"));

        URL url = mServer.url("/").url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Content-Length", "4");
        InputStream inputStream;
        MockOktaHttpClient httpClient = new MockOktaHttpClient(connection);
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            inputStream = connection.getErrorStream();
        }
        HttpResponse response = new HttpResponse(HTTP_OK, httpClient.getHeaderFields(),
                httpClient.getContentLength(), inputStream, httpClient);
        Map<String, List<String>> headers = response.getHeaders();
        InputStream in = response.getContent();
        int contentLength = response.getContentLength();
        int status = response.getStatusCode();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        assertEquals(reader.readLine(), "test");
        assertEquals(HTTP_OK, status);
        assertNotNull(headers);
        assertEquals(contentLength, 4);

        RecordedRequest request = mServer.takeRequest();
        assertEquals(request.getRequestLine(), "GET / HTTP/1.1");
        assertEquals(request.getHeader("Accept-Language"), "en-US");
    }

    @Test
    public void textErrorResponse() throws Exception {
        mExpectedEx.expect(IOException.class);
        mServer.enqueue(new MockResponse().setResponseCode(HTTP_FORBIDDEN)
                .addHeader(CONTENT_TYPE, "text/plain")
                .addHeader(WWW_AUTHENTICATE, FORBIDDEN)
                .setBody("Forbidden"));

        URL url = mServer.url("/").url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Content-Length", "9");
        MockOktaHttpClient httpClient = new MockOktaHttpClient(connection);
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            inputStream = connection.getErrorStream();
        }
        HttpResponse response = new HttpResponse(HTTP_FORBIDDEN, httpClient.getHeaderFields(),
                httpClient.getContentLength(), inputStream, httpClient);

        Map<String, List<String>> headers = response.getHeaders();
        InputStream in = response.getContent();
        int contentLength = response.getContentLength();
        int status = response.getStatusCode();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        assertEquals(reader.readLine(), "Forbidden");
        assertEquals(HTTP_FORBIDDEN, status);
        assertNotNull(headers);
        assertTrue(headers.get(WWW_AUTHENTICATE).contains(FORBIDDEN));
        assertEquals(contentLength, 9);
    }

    @Test
    public void jsonOKResponse() throws Exception {
        mServer.enqueue(new MockResponse().setResponseCode(HTTP_OK)
                .addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE)
                .setBody(TOKEN_SUCCESS));

        URL url = mServer.url("/").url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Content-Length", TOKEN_SUCCESS.length() + "");
        MockOktaHttpClient httpClient = new MockOktaHttpClient(connection);
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            inputStream = connection.getErrorStream();
        }
        HttpResponse response = new HttpResponse(HTTP_OK, httpClient.getHeaderFields(),
                httpClient.getContentLength(), inputStream, httpClient);

        Map<String, List<String>> headers = response.getHeaders();
        int contentLength = response.getContentLength();
        int status = response.getStatusCode();
        JSONObject jsonObject = response.asJson();
        TokenResponse tokenResponse =
                new Gson().fromJson(jsonObject.toString(), TokenResponse.class);

        assertNotNull(tokenResponse);
        assertEquals(HTTP_OK, status);
        assertNotNull(headers);
        assertEquals(contentLength, TOKEN_SUCCESS.length());

    }

    @Test
    public void jsonErrorResponse() throws Exception {
        mExpectedEx.expect(IOException.class);
        mServer.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND)
                .addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE)
                .setBody(CONFIGURATION_NOT_FOUND));

        URL url = mServer.url("/").url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Content-Length", CONFIGURATION_NOT_FOUND.length() + "");
        MockOktaHttpClient httpClient = new MockOktaHttpClient(connection);
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            inputStream = connection.getErrorStream();
        }
        HttpResponse response = new HttpResponse(HTTP_NOT_FOUND, httpClient.getHeaderFields(),
                httpClient.getContentLength(), inputStream, httpClient);

        Map<String, List<String>> headers = response.getHeaders();
        InputStream in = response.getContent();
        int contentLength = response.getContentLength();
        int status = response.getStatusCode();

        assertEquals(HTTP_NOT_FOUND, status);
        assertNotNull(headers);
        assertEquals(contentLength, CONFIGURATION_NOT_FOUND.length());
    }

    @Test
    public void redirectResponse() throws Exception {
        mServer.enqueue(new MockResponse().setResponseCode(HTTP_MOVED_TEMP)
                .addHeader("Location", "com.okta.test:/callback?code="
                        + SESSION_TOKEN));
        URL url = mServer.url("/").url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        MockOktaHttpClient httpClient = new MockOktaHttpClient(connection);

        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            inputStream = connection.getErrorStream();
        }

        HttpResponse response = new HttpResponse(HTTP_MOVED_TEMP, httpClient.getHeaderFields(),
                httpClient.getContentLength(), inputStream, httpClient);
        String location = response.getHeaderField("Location");
        Uri locationUri = Uri.parse(location);
        String code = locationUri.getQueryParameter("code");
        assertNotNull(location);
        assertNotNull(code);
        assertEquals(code, SESSION_TOKEN);
    }

    private class MockOktaHttpClient implements OktaHttpClient {
        private HttpURLConnection mConnection;

        public MockOktaHttpClient(HttpURLConnection connection) {
            mConnection = connection;
        }

        @Nullable
        @Override
        public InputStream connect(@NonNull Uri uri, @NonNull ConnectionParameters param) throws Exception {
            return null;
        }

        @Override
        public void cleanUp() {
            mConnection = null;
        }

        @Override
        public void cancel() {
            mConnection.disconnect();
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return mConnection.getHeaderFields();
        }

        @Override
        public String getHeader(String header) {
            return mConnection.getHeaderField(header);
        }

        @Override
        public int getResponseCode() throws IOException {
            return mConnection.getResponseCode();
        }

        @Override
        public int getContentLength() {
            return mConnection.getContentLength();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return mConnection.getResponseMessage();
        }
    }
}