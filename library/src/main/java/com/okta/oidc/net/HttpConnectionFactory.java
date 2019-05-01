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

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * For building customized {@link java.net.HttpURLConnection} instances for interacting directly
 * with Okta OIDC endpoints. For in-app connections only. Not in use for chrome custom tab.
 */
public interface HttpConnectionFactory {

    /**
     * Creates a connection to the specified URL.
     *
     * @param url the url
     * @return the http url connection
     * @throws IOException if an error occurs while attempting to establish the connection.
     */
    @NonNull
    HttpURLConnection build(@NonNull URL url) throws IOException;
}
