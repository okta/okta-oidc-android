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

package com.okta.oidc.example;

import androidx.annotation.NonNull;

import com.okta.oidc.net.OktaHttpClient;

/**
 * Simple connection factory.
 */
public class MyConnectionFactory {
    /**
     * The constant USE_OK_HTTP.
     */
    public static final int USE_OK_HTTP = 1;
    /**
     * The constant USE_SYNC_OK_HTTP.
     */
    public static final int USE_SYNC_OK_HTTP = 2;

    private int clientType = 0;

    /**
     * Build okta http client.
     *
     * @return the okta http client
     */
    @NonNull
    public OktaHttpClient build() {
        switch (clientType) {
            case USE_OK_HTTP:
                return new OkHttp();
            case USE_SYNC_OK_HTTP:
                return new SyncOkHttp();
            default:
                return new OkHttp();
        }
    }

    /**
     * Sets client type.
     *
     * @param clientType the client type
     */
    public void setClientType(int clientType) {
        this.clientType = clientType;
    }
}
