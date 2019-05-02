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

package com.okta.oidc.clients.sessions;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.okta.oidc.OIDCConfig;
import com.okta.oidc.OktaState;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.Tokens;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.net.request.AuthorizedRequest;
import com.okta.oidc.net.request.IntrospectRequest;
import com.okta.oidc.net.request.RevokeTokenRequest;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Executor;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SessionClientImpl implements SessionClient {
    private SyncSessionClientImpl mSyncSessionClientImpl;
    private RequestDispatcher mDispatcher;

    public SessionClientImpl(Executor callbackExecutor, OIDCConfig oidcConfig, OktaState oktaState,
                             HttpConnectionFactory connectionFactory) {
        mSyncSessionClientImpl =
                new SyncSessionClientImpl(oidcConfig, oktaState, connectionFactory);
        mDispatcher = new RequestDispatcher(callbackExecutor);
    }

    public void getUserProfile(final RequestCallback<UserInfo, AuthorizationException> cb) {
        AuthorizedRequest request = mSyncSessionClientImpl.userProfileRequest();
        request.dispatchRequest(mDispatcher,
                new RequestCallback<JSONObject, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull JSONObject result) {
                        cb.onSuccess(new UserInfo(result));
                    }

                    @Override
                    public void onError(String error, AuthorizationException exception) {
                        cb.onError(error, exception);
                    }
                });
    }

    public void introspectToken(String token, String tokenType,
                                final RequestCallback<IntrospectInfo, AuthorizationException> cb) {
        IntrospectRequest request = mSyncSessionClientImpl.introspectTokenRequest(token, tokenType);
        request.dispatchRequest(mDispatcher, cb);
    }

    public void revokeToken(String token,
                            final RequestCallback<Boolean, AuthorizationException> cb) {
        RevokeTokenRequest request = mSyncSessionClientImpl.revokeTokenRequest(token);
        request.dispatchRequest(mDispatcher, cb);
    }

    public void refreshToken(final RequestCallback<Tokens, AuthorizationException> cb) {
        //Wrap the callback from the app because we want to be consistent in
        //returning a Tokens object instead of a TokenResponse.
        mSyncSessionClientImpl.refreshTokenRequest().dispatchRequest(mDispatcher,
                new RequestCallback<TokenResponse, AuthorizationException>() {
                    @Override
                    public void onSuccess(@NonNull TokenResponse result) {
                        mSyncSessionClientImpl.getOktaState().save(result);
                        cb.onSuccess(new Tokens(result));
                    }

                    @Override
                    public void onError(String error, AuthorizationException exception) {
                        cb.onError(error, exception);
                    }
                });
    }

    public Tokens getTokens() {
        return mSyncSessionClientImpl.getTokens();
    }

    @Override
    public void authorizedRequest(@NonNull Uri uri, @Nullable Map<String, String> properties,
                                  @Nullable Map<String, String> postParameters,
                                  @NonNull HttpConnection.RequestMethod method,
                                  final RequestCallback<JSONObject, AuthorizationException> cb) {
        AuthorizedRequest request = mSyncSessionClientImpl.createAuthorizedRequest(uri, properties,
                postParameters, method);
        request.dispatchRequest(mDispatcher, cb);
    }

    public boolean isLoggedIn() {
        return mSyncSessionClientImpl.isLoggedIn();
    }

    public void clear() {
        mSyncSessionClientImpl.clear();
    }
}
