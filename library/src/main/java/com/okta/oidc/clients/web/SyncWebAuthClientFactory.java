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

package com.okta.oidc.clients.web;

import androidx.annotation.RestrictTo;

import com.okta.oidc.Okta;
import com.okta.oidc.clients.ClientFactory;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SyncWebAuthClientFactory implements ClientFactory<SyncWebAuthClient> {
    private Okta.SyncWebAuthBuilder mBuilder;

    public SyncWebAuthClientFactory(Okta.SyncWebAuthBuilder builder) {
        mBuilder = builder;
    }

    @Override
    public SyncWebAuthClient createClient() {
        return new SyncWebAuthClientImpl(mBuilder);
    }
}
