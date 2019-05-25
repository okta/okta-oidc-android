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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The interface Http client for making network connections.
 */
public interface OktaHttpClient {
    /**
     * Connect to the url provided in connection parameters.
     *
     * @param uri   uri for the connection.
     * @param param parameters for the connection.
     * @return a InputStream.
     * @throws Exception the exception indicating failure case.
     */
    @Nullable
    InputStream connect(@NonNull Uri uri, @NonNull ConnectionParameters param) throws Exception;

    /**
     * Clean up any resources in OktaHttpClient.
     */
    void cleanUp();

    /**
     * Attempt to cancel a connection.
     */
    void cancel();

    /**
     * Gets header fields.
     *
     * @return the header fields.
     */
    Map<String, List<String>> getHeaderFields();

    /**
     * Get header.
     *
     * @param header the header.
     * @return the single header.
     */
    String getHeader(String header);

    /**
     * Gets response code.
     *
     * @return the response code.
     * @throws IOException the exception indicating connection error.
     */
    int getResponseCode() throws IOException;

    /**
     * Gets content length.
     *
     * @return the content length.
     */
    int getContentLength();

    /**
     * Gets the Http status message.
     *
     * @return the response message.
     * @throws IOException the exception indicating connection error.
     */
    String getResponseMessage() throws IOException;
}
