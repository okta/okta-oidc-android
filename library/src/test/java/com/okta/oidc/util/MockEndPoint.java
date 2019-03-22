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
package com.okta.oidc.util;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.internal.TlsUtil;

import static com.okta.oidc.net.HttpConnection.CONTENT_TYPE;
import static com.okta.oidc.net.HttpConnection.JSON_CONTENT_TYPE;
import static com.okta.oidc.util.JsonStrings.CONFIGURATION_NOT_FOUND;
import static com.okta.oidc.util.JsonStrings.FORBIDDEN;
import static com.okta.oidc.util.JsonStrings.INVALID_CLIENT;
import static com.okta.oidc.util.JsonStrings.PROVIDER_CONFIG;
import static com.okta.oidc.util.JsonStrings.UNAUTHORIZED_INVALID_TOKEN;
import static com.okta.oidc.util.JsonStrings.USER_PROFILE;
import static com.okta.oidc.util.JsonStrings.WWW_AUTHENTICATE;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class MockEndPoint {
    private MockWebServer mServer;
    private Gson mGson;

    public MockEndPoint() throws IOException, NoSuchAlgorithmException {
        mServer = new MockWebServer();
        SSLSocketFactory sslSocketFactory = getSSL();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        mServer.useHttps(getSSL(), false);
        mServer.start();
        mGson = new Gson();
    }

    public String getUrl() {
        return mServer.url("/").toString();
    }

    public void shutDown() throws IOException {
        mServer.shutdown();
    }

    public RecordedRequest takeRequest() throws InterruptedException {
        return mServer.takeRequest();
    }

    public void enqueueUserInfoSuccess() {
        mServer.enqueue(jsonResponse(HTTP_OK, USER_PROFILE));
    }

    public void enqueueConfigurationSuccess() {
        mServer.enqueue(jsonResponse(HTTP_OK, PROVIDER_CONFIG));
    }

    public void enqueueConfigurationFailure() {
        mServer.enqueue(jsonResponse(HTTP_NOT_FOUND, CONFIGURATION_NOT_FOUND));
    }

    public void enqueueReturnInvalidClient() {
        mServer.enqueue(jsonResponse(HTTP_UNAUTHORIZED, INVALID_CLIENT));
    }


    public void enqueueReturnSuccessEmptyBody() {
        mServer.enqueue(emptyResponse(HTTP_OK));
    }

    public void enqueueReturnUnauthorizedRevoked() {
        MockResponse response = textResponse(HTTP_UNAUTHORIZED, "Unauthorized")
                .addHeader(WWW_AUTHENTICATE, UNAUTHORIZED_INVALID_TOKEN);
        mServer.enqueue(response);
    }

    public void enqueueForbidden() {
        MockResponse response = textResponse(HTTP_FORBIDDEN, "Forbidden")
                .addHeader(WWW_AUTHENTICATE, FORBIDDEN);
        mServer.enqueue(response);
    }

    private MockResponse emptyResponse(int code) {
        return new MockResponse().setResponseCode(code);
    }


    private MockResponse textResponse(int code, String status) {
        return new MockResponse().setResponseCode(code)
                .addHeader(CONTENT_TYPE, "text/plain")
                .setBody(status);
    }

    private MockResponse jsonResponse(int code, String json) {
        return new MockResponse().setResponseCode(code)
                .addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE)
                .setBody(json);
    }

    private SSLSocketFactory getSSL() {
        try {
            /*
             * To generate keystore you should use next command
             * keytool -genkey -v -keystore mock.keystore.jks -alias okta_android_sdk -keyalg RSA -keysize 2048 -validity 10000
             * Copy mock.keystore.jks in folder library/src/test/resources
             * */
            URL filepath = getClass().getClassLoader().getResource("mock.keystore.jks");
            File file = new File(filepath.getPath());

            FileInputStream stream = new FileInputStream(file);
            char[] serverKeyStorePassword = "123456".toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            serverKeyStore.load(stream, serverKeyStorePassword);

            String kmfAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlgorithm);
            kmf.init(serverKeyStore, serverKeyStorePassword);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(kmfAlgorithm);
            trustManagerFactory.init(serverKeyStore);

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(kmf.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            return null;
        }
    }
}
