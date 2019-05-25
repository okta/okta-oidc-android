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

import androidx.annotation.NonNull;

import com.okta.oidc.net.HttpClientImpl;
import com.okta.oidc.net.OktaHttpClient;

public class HttpClientFactory {
    public static final int USE_DEFAULT_HTTP = 0;
    public static final int USE_OK_HTTP = 1;
    public static final int USE_SYNC_OK_HTTP = 2;

    private int clientType = 0;

    @NonNull
    public OktaHttpClient build() {
        switch (clientType) {
            case USE_DEFAULT_HTTP:
                return new HttpClientImpl();
            case USE_OK_HTTP:
                return new OkHttp();
            case USE_SYNC_OK_HTTP:
                return new SyncOkHttp();
            default:
                return new HttpClientImpl();
        }
    }

    public void setClientType(int clientType) {
        this.clientType = clientType;
    }
}
