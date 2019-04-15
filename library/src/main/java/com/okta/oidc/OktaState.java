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

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.Persistable;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
class OktaState {
    private OktaRepository mOktaRepo;

    OktaState(OktaRepository oktaRepository) {
        mOktaRepo = oktaRepository;
    }

    TokenResponse getTokenResponse() {
        return mOktaRepo.get(TokenResponse.RESTORE);
    }

    ProviderConfiguration getProviderConfiguration() {
        return mOktaRepo.get(ProviderConfiguration.RESTORE);
    }

    WebRequest getAuthorizeRequest() {
        return mOktaRepo.get(WebRequest.RESTORE);
    }

    void save(Persistable persistable) {
        mOktaRepo.save(persistable);
    }

    void delete(Persistable persistable) {
        mOktaRepo.delete(persistable);
    }
}
