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

import androidx.annotation.AnyThread;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.sessions.SessionClientImpl;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.results.AuthorizationResult;
import com.okta.oidc.util.AuthorizationException;

import java.util.concurrent.Executor;

class AuthClientImpl implements AuthClient {
    private RequestDispatcher mDispatcher;
    private SyncAuthClientImpl mSyncNativeAuthClient;
    private SessionClientImpl mSessionImpl;

    AuthClientImpl(Executor executor, OIDCConfig oidcConfig, OktaState oktaState,
                   HttpConnectionFactory httpConnectionFactory) {
        mSyncNativeAuthClient = new SyncAuthClientImpl(oidcConfig, oktaState,
                httpConnectionFactory);
        mSessionImpl = new SessionClientImpl(executor, oidcConfig, oktaState,
                httpConnectionFactory);
        mDispatcher = new RequestDispatcher(executor);
    }

    @Override
    @AnyThread
    public void logIn(String sessionToken, AuthenticationPayload payload,
                      RequestCallback<AuthorizationResult, AuthorizationException> cb) {
        mDispatcher.execute(() -> {
            AuthorizationResult result = mSyncNativeAuthClient.logIn(sessionToken, payload);
            if (result.isSuccess()) {
                mDispatcher.submitResults(() -> {
                    if (cb != null) {
                        cb.onSuccess(result);
                    }
                });
            } else {
                mDispatcher.submitResults(() -> {
                    if (cb != null) {
                        cb.onError(result.getError().error, result.getError());
                    }
                });
            }
        });
    }

    @Override
    public SessionClient getSessionClient() {
        return mSessionImpl;
    }
}
