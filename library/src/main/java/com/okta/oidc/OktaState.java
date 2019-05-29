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

import androidx.annotation.RestrictTo;

import com.okta.oidc.clients.State;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.Persistable;
import com.okta.oidc.storage.security.EncryptionManager;

import static com.okta.oidc.clients.State.IDLE;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class OktaState {
    private OktaRepository mOktaRepo;

    private State currentState;

    public OktaState(OktaRepository mOktaRepository) {
        this.mOktaRepo = mOktaRepository;
        this.currentState = IDLE;
    }

    public TokenResponse getTokenResponse() throws OktaRepository.EncryptionException {
        return mOktaRepo.get(TokenResponse.RESTORE);
    }

    public boolean hasTokenResponse() {
        return mOktaRepo.contains(TokenResponse.RESTORE);
    }

    public ProviderConfiguration getProviderConfiguration() throws OktaRepository.EncryptionException {
        return mOktaRepo.get(ProviderConfiguration.RESTORE);
    }

    public WebRequest getAuthorizeRequest() throws OktaRepository.EncryptionException {
        return mOktaRepo.get(WebRequest.RESTORE);
    }

    public void setCurrentState(State state) {
        this.currentState = state;
    }

    public State getCurrentState() {
        return this.currentState;
    }

    public void save(Persistable persistable) throws OktaRepository.EncryptionException {
        mOktaRepo.save(persistable);
    }

    public void delete(Persistable persistable) {
        mOktaRepo.delete(persistable);
    }

    public void delete(String key) {
        mOktaRepo.delete(key);
    }

    public void setEncryptionManager(EncryptionManager encryptionManager) {
        this.mOktaRepo.setEncryptionManager(encryptionManager);
    }
}
