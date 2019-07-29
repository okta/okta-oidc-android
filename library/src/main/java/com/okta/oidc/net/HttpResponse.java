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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class HttpResponse {
    private static final String TAG = HttpResponse.class.getSimpleName();
    private final int mStatusCode;
    private final Map<String, List<String>> mHeaders;
    private final int mLength;
    private final OktaHttpClient mHttpClient;
    private InputStream mInputStream;
    private static final int BUFFER_SIZE = 1024;

    /**
     * HttpResponse for empty response body.
     *
     * @param statusCode HTTP status code
     * @param headers    response headers
     */
    public HttpResponse(int statusCode, Map<String, List<String>> headers) {
        this(statusCode, headers, -1, null, null);
    }

    /**
     * Constructor for HttpResponse.
     *
     * @param statusCode HTTP status code of the response
     * @param headers    response headers
     * @param length     the length of the response.
     * @param client     an {@link OktaHttpClient} the OktaHttpClient
     */
    public HttpResponse(
            int statusCode, Map<String, List<String>> headers,
            int length, InputStream inputStream, OktaHttpClient client) {
        mStatusCode = statusCode;
        mHeaders = headers;
        mLength = length;
        mHttpClient = client;
        mInputStream = inputStream;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(mHeaders);
    }

    public String getHeaderField(String field) {
        return mHttpClient.getHeader(field);
    }

    public int getContentLength() {
        return mLength;
    }

    @Nullable
    public InputStream getContent() throws IOException {
        if (mStatusCode < HttpURLConnection.HTTP_OK ||
                mStatusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            throw new IOException("Invalid status code " + mStatusCode +
                    " " + mHttpClient.getResponseMessage());
        }
        return mInputStream;
    }

    public void disconnect() {
        if (mHttpClient != null) {
            mHttpClient.cleanUp();
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException ioe) {
                //NO-OP
            }
        }
    }

    public JSONObject asJson() throws IOException, JSONException {
        InputStream is = getContent();
        if (is == null) {
            throw new IOException("Input stream must not be null");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Writer writer = new StringWriter();
        String line = reader.readLine();
        while (line != null) {
            writer.write(line);
            line = reader.readLine();
        }
        return new JSONObject(writer.toString());
    }

    public byte[] getByteArray() throws IOException {
        InputStream is = getContent();
        if (is == null) {
            throw new IOException("Input stream must not be null");
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = is.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
