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

package com.okta.oidc.clients;

import android.content.Context;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.security.EncryptionManager;

/**
 * The interface Auth client factory. Used to create a auth client.
 *
 * @param <A> the type of client to create
 */
public interface ClientFactory<A> {
    /**
     * Create client a.
     *
     * @param oidcConfig        the oidc config
     * @param context           the context
     * @param oktaStorage       the storage
     * @param encryptionManager the encryption manager
     * @param connectionFactory the connection factory
     * @return the type of auth client
     */
    A createClient(OIDCConfig oidcConfig,
                   Context context,
                   OktaStorage oktaStorage,
                   EncryptionManager encryptionManager,
                   HttpConnectionFactory connectionFactory);
}
