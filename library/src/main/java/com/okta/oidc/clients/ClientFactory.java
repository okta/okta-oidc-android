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

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.clients.sessions.SyncSessionClient;
import com.okta.oidc.clients.web.SyncWebAuthClient;
import com.okta.oidc.net.HttpConnectionFactory;

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
     * @param oktaState         the okta state
     * @param connectionFactory the connection factory
     * @return the type of auth client
     */
    A createClient(OIDCConfig oidcConfig, OktaState oktaState,
                   HttpConnectionFactory connectionFactory);
}
