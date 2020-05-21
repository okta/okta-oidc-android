/*
 * Copyright (c) 2020, Okta, Inc. and/or its affiliates. All rights reserved.
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

import androidx.annotation.NonNull;

import java.io.IOException;

/**
 * Thrown when an unexpected status code is encountered during
 * serialization of an HTTP response.
 */
public class HttpStatusCodeException extends IOException {
    private final int statusCode;
    private final String statusMessage;

    /**
     * Instantiates an HttpStatusCodeException with the given HTTP status code, and
     * status message.
     *
     * @param statusCode The HTTP status code that was not expected
     * @param statusMessage The message contained in the response HTTP message
     */
    public HttpStatusCodeException(int statusCode, @NonNull String statusMessage) {
        super("Invalid status code " + statusCode +
                " " + statusMessage);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the HTTP status code that was unexpected when
     * this exception occurred.
     * @return The HTTP status code
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Returns the HTTP status message contained in the HTTP response
     * when this exception occurred.
     * @return The HTTP status message
     */
    @NonNull
    public String getStatusMessage() {
        return this.statusMessage;
    }
}
