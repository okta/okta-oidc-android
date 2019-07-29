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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.okta.oidc.BuildConfig;
import com.okta.oidc.net.params.RequestType;
import com.okta.oidc.util.Preconditions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The type Connection parameters.
 */
public class ConnectionParameters {
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 15000; //15s
    private static final int DEFAULT_READ_TIMEOUT_MS = 10000; //10s
    /**
     * The constant DEFAULT_ENCODING. "UTF-8"
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    /**
     * The constant CONTENT_TYPE.
     */
    public static final String CONTENT_TYPE = "Content-Type";
    /**
     * The constant DEFAULT_CONTENT_TYPE.
     */
    public static final String DEFAULT_CONTENT_TYPE =
            String.format("application/x-www-form-urlencoded; charset=%s", DEFAULT_ENCODING);
    /**
     * The constant JSON_CONTENT_TYPE.
     */
    public static final String JSON_CONTENT_TYPE = String.format("application/json; charset=%s",
            DEFAULT_ENCODING);
    /**
     * The constant USER_AGENT.
     */
    public static final String USER_AGENT = "User-Agent";
    /**
     * The constant X_OKTA_USER_AGENT.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String X_OKTA_USER_AGENT = "X-Okta-User-Agent-Extended";
    /**
     * The constant USER_AGENT_HEADER.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String USER_AGENT_HEADER = "okta-oidc-android/" + Build.VERSION.SDK_INT +
            " " + BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME;

    /**
     * The enum Request method.
     */
    public enum RequestMethod {
        /**
         * Get request method.
         */
        GET,
        /**
         * Post request method.
         */
        POST
    }

    private RequestMethod mRequestMethod;
    private Map<String, String> mRequestProperties;
    private Map<String, String> mPostParameters;
    private int mConnectionTimeoutMs;
    private int mReadTimeOutMs;
    private RequestType mRequestType;
    private byte[] mRequestBody;

    /**
     * Instantiates a new Connection parameters.
     *
     * @param builder the builder
     */
    ConnectionParameters(ParameterBuilder builder) {
        mRequestMethod = builder.mRequestMethod;
        mRequestProperties = builder.mRequestProperties;
        mReadTimeOutMs = builder.mReadTimeOutMs;
        mConnectionTimeoutMs = builder.mConnectionTimeoutMs;
        mPostParameters = builder.mPostParameters;
        mRequestType = builder.mRequestType;
        mRequestBody = builder.mRequestBody;
    }

    private byte[] encodePostParameters() {
        StringBuilder encodedParams = new StringBuilder();
        try {
            Iterator<Map.Entry<String, String>> iterator = mPostParameters.entrySet().iterator();
            while (iterator.hasNext()) {
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

    /**
     * Request method request method.
     *
     * @return the request method
     */
    public RequestMethod requestMethod() {
        return mRequestMethod;
    }

    /**
     * Request properties map.
     *
     * @return the map
     */
    public Map<String, String> requestProperties() {
        mRequestProperties.put(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        mRequestProperties.put(USER_AGENT, USER_AGENT_HEADER);
        return mRequestProperties;
    }

    /**
     * Post parameters map.
     *
     * @return the map
     */
    public Map<String, String> postParameters() {
        return mPostParameters;
    }

    /**
     * Connection timeout ms int.
     *
     * @return the int
     */
    public int connectionTimeoutMs() {
        return mConnectionTimeoutMs > 0 ? mConnectionTimeoutMs : DEFAULT_CONNECTION_TIMEOUT_MS;
    }

    /**
     * Read time out ms int.
     *
     * @return the int
     */
    public int readTimeOutMs() {
        return mReadTimeOutMs > 0 ? mReadTimeOutMs : DEFAULT_READ_TIMEOUT_MS;
    }

    /**
     * Get encoded post parameters byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getEncodedPostParameters() {
        return encodePostParameters();
    }

    /**
     * Get the post request body.
     *
     * @return the request body
     */
    public byte[] getBody() {
        return mRequestBody;
    }

    /**
     * Get the request type.
     *
     * @return the request type {@link RequestType}
     */
    public RequestType getRequestType() {
        return mRequestType;
    }

    /**
     * The type Parameter builder.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final class ParameterBuilder {
        private RequestMethod mRequestMethod;
        private Map<String, String> mRequestProperties;
        private Map<String, String> mPostParameters;
        private int mConnectionTimeoutMs = -1;
        private int mReadTimeOutMs = -1;
        private RequestType mRequestType;
        private byte[] mRequestBody;

        /**
         * Instantiates a new Parameter builder.
         */
        public ParameterBuilder() {
        }

        /**
         * Create connection parameters.
         *
         * @return the connection parameters
         */
        public ConnectionParameters create() {
            Preconditions.checkNotNull(mRequestMethod);
            if (mRequestProperties == null) {
                mRequestProperties = new HashMap<>();
            }
            return new ConnectionParameters(this);
        }

        /**
         * Sets request method.
         *
         * @param method the method
         * @return the request method
         */
        public ParameterBuilder setRequestMethod(@NonNull RequestMethod method) {
            mRequestMethod = method;
            return this;
        }

        /**
         * Sets request property.
         *
         * @param key   the key
         * @param value the value
         * @return the request property
         */
        public ParameterBuilder setRequestProperty(@NonNull String key,
                                                   @NonNull String value) {
            if (mRequestProperties == null) {
                mRequestProperties = new HashMap<>();
            }
            mRequestProperties.put(key, value);
            return this;
        }

        /**
         * Sets request properties.
         *
         * @param map the map
         * @return the builder
         */
        public ParameterBuilder setRequestProperties(@NonNull Map<String, String> map) {
            if (mRequestProperties == null) {
                mRequestProperties = new HashMap<>();
            }
            mRequestProperties.putAll(map);
            return this;
        }

        /**
         * Sets connection timeout ms.
         *
         * @param timeOut the time out
         * @return the builder
         */
        public ParameterBuilder setConnectionTimeoutMs(int timeOut) {
            mConnectionTimeoutMs = timeOut;
            return this;
        }

        /**
         * Sets read time out ms.
         *
         * @param readTimeOut the read time out
         * @return the builder
         */
        public ParameterBuilder setReadTimeOutMs(int readTimeOut) {
            mReadTimeOutMs = readTimeOut;
            return this;
        }

        /**
         * Sets post parameter.
         *
         * @param key   the key
         * @param value the value
         * @return the builder
         */
        public ParameterBuilder setPostParameter(@NonNull String key, @NonNull String value) {
            if (mPostParameters == null) {
                mPostParameters = new HashMap<>();
            }
            mPostParameters.put(key, value);
            return this;
        }

        /**
         * Sets post parameters.
         *
         * @param map the map
         * @return the builder
         */
        public ParameterBuilder setPostParameters(@NonNull Map<String, String> map) {
            if (mPostParameters == null) {
                mPostParameters = new HashMap<>();
            }
            mPostParameters.putAll(map);
            return this;
        }

        /**
         * Sets request type.
         *
         * @param type the type {@link RequestType}
         * @return the builder
         */
        public ParameterBuilder setRequestType(@NonNull RequestType type) {
            mRequestType = type;
            return this;
        }

        /**
         * Sets the request body.
         *
         * @param requestBody the request body
         * @return the builder
         */
        public ParameterBuilder setRequestBody(@NonNull byte[] requestBody) {
            mRequestBody = requestBody;
            return this;
        }
    }
}
