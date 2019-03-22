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

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.okta.oidc.BuildConfig;
import com.okta.oidc.net.request.TLSSocketFactory;
import com.okta.oidc.util.Preconditions;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class HttpConnection {
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String DEFAULT_CONTENT_TYPE =
            String.format("application/x-www-form-urlencoded; charset=%s", DEFAULT_ENCODING);
    public static final String JSON_CONTENT_TYPE = String.format("application/json; charset=%s",
            DEFAULT_ENCODING);
    public static final String USER_AGENT = "User-Agent";
    public static final String USER_AGENT_HEADER = "Android/" + Build.VERSION.SDK_INT + " " +
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME;

    public enum RequestMethod {
        GET, POST
    }

    private RequestMethod mRequestMethod;
    private Map<String, String> mRequestProperties;
    private Map<String, String> mPostParameters;
    private int mConnectionTimeoutMs;
    private int mReadTimeOutMs;
    private HttpConnectionFactory mConnectionFactory;

    private HttpConnection(Builder builder) {
        mRequestMethod = builder.mRequestMethod;
        mRequestProperties = builder.mRequestProperties;
        mReadTimeOutMs = builder.mReadTimeOutMs;
        mConnectionTimeoutMs = builder.mConnectionTimeoutMs;
        mPostParameters = builder.mPostParameters;
        mConnectionFactory = builder.mConnectionFactory;
    }

    @NonNull
    public HttpURLConnection openConnection(@NonNull URL url) throws IOException {
        HttpURLConnection conn = mConnectionFactory.build(url);
        conn.setConnectTimeout(mConnectionTimeoutMs);
        conn.setReadTimeout(mReadTimeOutMs);
        conn.setInstanceFollowRedirects(false);

        if (mRequestProperties == null || !mRequestProperties.containsKey(USER_AGENT)) {
            conn.setRequestProperty(USER_AGENT, USER_AGENT_HEADER);
        }
        if (mRequestProperties == null || !mRequestProperties.containsKey(CONTENT_TYPE)) {
            conn.setRequestProperty(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        }
        if (mRequestProperties != null) {
            for (String property : mRequestProperties.keySet()) {
                conn.setRequestProperty(property, mRequestProperties.get(property));
            }
        }
        if (mRequestMethod == RequestMethod.GET) {
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
        } else if (mRequestMethod == RequestMethod.POST) {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            if (mPostParameters != null && !mPostParameters.isEmpty()) {
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.write(encodePostParameters());
                out.close();
            }
        }
        return conn;
    }

    private byte[] encodePostParameters() {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Iterator<Map.Entry<String, String>> iterator =
                 mPostParameters.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, String> entry = iterator.next();
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "mPostParameters returned a map "
                                            + "containing a null key or value: (%s, %s).",
                                    entry.getKey(), entry.getValue()));
                }
                encodedParams.append(URLEncoder.encode(entry.getKey(), DEFAULT_ENCODING));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), DEFAULT_ENCODING));
                if (iterator.hasNext()) {
                    encodedParams.append('&');
                }
            }
            return encodedParams.toString().getBytes(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + DEFAULT_ENCODING, uee);
        }
    }

    private static final class DefaultConnectionFactory implements HttpConnectionFactory {
        /*
        * TLS v1.1, v1.2 in Android supports starting from API 16. But it enabled by default starting
        * from API 20.
        * This method enable these TLS versions on API < 20.
        * */
        private void enableTLSv1_2(HttpURLConnection urlConnection) {
            try {
                ((HttpsURLConnection) urlConnection)
                            .setSSLSocketFactory(new TLSSocketFactory());
            } catch ( NoSuchAlgorithmException | KeyManagementException e ) {
                throw new RuntimeException("Cannot create SSLContext.", e);
            }
        }

        @NonNull
        @Override
        public HttpURLConnection build(@NonNull URL url) throws IOException {
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            if ( urlConnection instanceof HttpsURLConnection &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ) {
                enableTLSv1_2(urlConnection);
            }
            return urlConnection;
        }
    }

    public static final class Builder {
        private RequestMethod mRequestMethod;
        private Map<String, String> mRequestProperties;
        private Map<String, String> mPostParameters;
        private int mConnectionTimeoutMs = (int) TimeUnit.SECONDS.toMillis(15);
        private int mReadTimeOutMs = (int) TimeUnit.SECONDS.toMillis(10);
        private HttpConnectionFactory mConnectionFactory;

        public Builder() {
        }

        public HttpConnection create() {
            return create(null);
        }

        public HttpConnection create(HttpConnectionFactory connectionFactory) {
            Preconditions.checkNotNull(mRequestMethod);
            if (connectionFactory == null) {
                mConnectionFactory = new DefaultConnectionFactory();
            }
            return new HttpConnection(this);
        }

        public HttpConnection.Builder setRequestMethod(@NonNull RequestMethod method) {
            mRequestMethod = method;
            return this;
        }

        public HttpConnection.Builder setRequestProperty(@NonNull String key,
                                                         @NonNull String value) {
            if (mRequestProperties == null) {
                mRequestProperties = new HashMap<>();
            }
            mRequestProperties.put(key, value);
            return this;
        }

        public HttpConnection.Builder setRequestProperties(@NonNull Map<String, String> map) {
            if (mRequestProperties == null) {
                mRequestProperties = new HashMap<>();
            }
            mRequestProperties.putAll(map);
            return this;
        }

        public HttpConnection.Builder setConnectionTimeoutMs(int timeOut) {
            mConnectionTimeoutMs = timeOut;
            return this;
        }

        public HttpConnection.Builder setReadTimeOutMs(int readTimeOut) {
            mReadTimeOutMs = readTimeOut;
            return this;
        }

        public HttpConnection.Builder setPostParameter(@NonNull String key, @NonNull String value) {
            if (mPostParameters == null) {
                mPostParameters = new HashMap<>();
            }
            mPostParameters.put(key, value);
            return this;
        }

        public HttpConnection.Builder setPostParameters(@NonNull Map<String, String> map) {
            if (mPostParameters == null) {
                mPostParameters = new HashMap<>();
            }
            mPostParameters.putAll(map);
            return this;
        }
    }
}
