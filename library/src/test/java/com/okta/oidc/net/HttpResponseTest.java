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

import com.okta.oidc.AuthenticateClient;
import com.okta.oidc.OIDCAccount;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.util.JsonStrings;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;

import static com.okta.oidc.net.HttpConnection.CONTENT_TYPE;
import static com.okta.oidc.net.HttpConnection.JSON_CONTENT_TYPE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HttpResponseTest {
    private HttpResponse mOkEmptyResponse;
    private HttpResponse mOkJsonResponse;
    private HttpResponse mInvalidJsonResponse;
    private HttpResponse mInvalidEmptyResponse;

    private MockEndPoint mEndPoint;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        setupOkEmptyResponse();
        setupOkJsonResponse();
        setupInvalidEmptyResponse();
        setupInvalidJsonResponse();
    }

    @Test
    public void getStatusCode() {
        int status = mOkEmptyResponse.getStatusCode();
        assertEquals(HTTP_OK, status);

        status = mOkJsonResponse.getStatusCode();
        assertEquals(HTTP_OK, status);

        status = mInvalidEmptyResponse.getStatusCode();
        assertEquals(HTTP_UNAUTHORIZED, status);

        status = mInvalidJsonResponse.getStatusCode();
        assertEquals(HTTP_NOT_FOUND, status);
    }

    @Test
    public void getHeaders() throws InterruptedException {
        Map<String, List<String>> headers = mOkEmptyResponse.getHeaders();
        assertNotNull(headers);
        headers = mOkJsonResponse.getHeaders();
        assertNotNull(headers);
        headers = mInvalidEmptyResponse.getHeaders();
        assertNotNull(headers);
        headers = mInvalidJsonResponse.getHeaders();
        assertNotNull(headers);
    }

    @Test
    public void getContentLength() {
        int contentLength = mOkEmptyResponse.getContentLength();
        System.out.println("content length = " + contentLength);
        contentLength = mOkJsonResponse.getContentLength();
        System.out.println("content length = " + contentLength);
        contentLength = mInvalidEmptyResponse.getContentLength();
        System.out.println("content length = " + contentLength);
        contentLength = mInvalidJsonResponse.getContentLength();
        System.out.println("content length = " + contentLength);
    }

    @Test
    public void getContent() {
        InputStream content = mOkJsonResponse.getContent();
        assertNotNull(content);
    }

    @Test
    public void disconnect() {
    }

    @Test
    public void asJson() {
    }

    private void setupOkEmptyResponse() throws IOException, InterruptedException {
        MockResponse mockResponse = mEndPoint.enqueueReturnSuccessEmptyBody();
        URL url = mEndPoint.getHttpUrl().url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        mOkEmptyResponse = new HttpResponse(HTTP_OK, mockResponse.getHeaders().toMultimap());
        //mEndPoint.takeRequest();
    }

    private void setupOkJsonResponse() throws IOException, InterruptedException {
        MockResponse mockResponse = mEndPoint.enqueueUserInfoSuccess();
        URL url = mEndPoint.getHttpUrl().url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Accept", HttpConnection.JSON_CONTENT_TYPE);

        mOkJsonResponse = new HttpResponse(HTTP_OK, mockResponse.getHeaders().toMultimap(),
                connection.getContentLength(), connection);
        //mEndPoint.takeRequest();
    }

    private void setupInvalidEmptyResponse() throws IOException, InterruptedException {
        MockResponse mockResponse = mEndPoint.enqueueReturnUnauthorizedRevoked();
        URL url = mEndPoint.getHttpUrl().url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        mInvalidEmptyResponse = new HttpResponse(HTTP_UNAUTHORIZED, mockResponse.getHeaders().toMultimap());
        mEndPoint.takeRequest();
    }

    private void setupInvalidJsonResponse() throws IOException, InterruptedException {
        MockResponse mockResponse = mEndPoint.enqueueConfigurationFailure();
        URL url = mEndPoint.getHttpUrl().url();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("Accept", HttpConnection.JSON_CONTENT_TYPE);
        mInvalidJsonResponse = new HttpResponse(HTTP_NOT_FOUND, mockResponse.getHeaders().toMultimap(),
                connection.getContentLength(), connection);
        mEndPoint.takeRequest();
    }


}