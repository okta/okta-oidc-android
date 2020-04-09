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

package com.okta.oidc;

import androidx.browser.customtabs.CustomTabsClient;

/**
 * Callback for events when connecting and disconnecting from Custom Tabs Service.
 */
/*package*/ interface ServiceConnectionCallback {
    /**
     * Called when the service is connected.
     * @param browserPackage browser package
     * @param client a CustomTabsClient
     */
    void onServiceConnected(String browserPackage,
                            CustomTabsClient client);

    /**
     * Called when the service is disconnected.
     */
    void onServiceDisconnected();
}
