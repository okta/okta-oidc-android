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

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.EncryptionManagerStub;
import com.okta.oidc.util.OktaStorageMock;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaStateTest {
    private OktaState mOktaState;
    private OktaStorageMock mOktaStorageMock;
    private OktaRepository mOktaRepository;
    Context mContext;


    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mOktaStorageMock = new OktaStorageMock(mContext, false);
        mOktaRepository = new OktaRepository(mOktaStorageMock, mContext, new EncryptionManagerStub());
        mOktaState = new OktaState(mOktaRepository);
    }

    @Test
    public void getAuthorizeRequest() throws AuthorizationException {
        WebRequest authorizedRequest = TestValues.getAuthorizeRequest(TestValues.getConfigWithUrl(CUSTOM_URL), null);
        mOktaState.save(authorizedRequest);

        AuthorizeRequest expected = (AuthorizeRequest) mOktaState.getAuthorizeRequest();
        assertNotNull(expected);
        assertEquals(authorizedRequest.persist(), expected.persist());
    }

    @Test
    public void getProviderConfiguration() {
        ProviderConfiguration providerConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mOktaState.save(providerConfiguration);

        ProviderConfiguration expected = mOktaState.getProviderConfiguration();

        assertNotNull(expected);
        assertEquals(providerConfiguration.persist(), expected.persist());
    }

    @Test
    public void getTokenResponse() {
        TokenResponse tokenResponse = TestValues.getTokenResponse();
        mOktaState.save(tokenResponse);
        TokenResponse expected = mOktaState.getTokenResponse();

        assertNotNull(expected);
        assertEquals(tokenResponse.persist(), expected.persist());
    }

    @Test
    public void validateDelete() {
        TokenResponse tokenResponse = TestValues.getTokenResponse();
        ProviderConfiguration providerConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mOktaState.save(tokenResponse);
        mOktaState.save(providerConfiguration);

        mOktaState.delete(tokenResponse);
        assertNull(mOktaState.getTokenResponse());
        assertNotNull(mOktaState.getProviderConfiguration());

    }
}
