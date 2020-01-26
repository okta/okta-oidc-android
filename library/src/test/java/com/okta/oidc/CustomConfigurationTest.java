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

import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.AUTHORIZATION_ENDPOINT;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.END_SESSION_ENDPOINT;
import static com.okta.oidc.util.TestValues.INTROSPECT_ENDPOINT;
import static com.okta.oidc.util.TestValues.JWKS_ENDPOINT;
import static com.okta.oidc.util.TestValues.REGISTRATION_ENDPOINT;
import static com.okta.oidc.util.TestValues.REVOCATION_ENDPOINT;
import static com.okta.oidc.util.TestValues.TOKEN_ENDPOINT;
import static com.okta.oidc.util.TestValues.USERINFO_ENDPOINT;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class CustomConfigurationTest {
    private CustomConfiguration mConfig;

    @Before
    public void setUp() throws Exception {
        mConfig = TestValues.getCustomConfiguration(CUSTOM_URL);
    }

    @Test
    public void getAuthorizationEndpoint() {
        String endpoint = mConfig.getAuthorizationEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + AUTHORIZATION_ENDPOINT, endpoint);
    }

    @Test
    public void getTokenEndpoint() {
        String endpoint = mConfig.getTokenEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + TOKEN_ENDPOINT, endpoint);
    }

    @Test
    public void getUserInfoEndpoint() {
        String endpoint = mConfig.getUserInfoEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + USERINFO_ENDPOINT, endpoint);
    }

    @Test
    public void getJwksUri() {
        String endpoint = mConfig.getJwksUri();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + JWKS_ENDPOINT, endpoint);
    }

    @Test
    public void getRegistrationEndpoint() {
        String endpoint = mConfig.getRegistrationEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + REGISTRATION_ENDPOINT, endpoint);
    }

    @Test
    public void getIntrospectionEndpoint() {
        String endpoint = mConfig.getIntrospectionEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + INTROSPECT_ENDPOINT, endpoint);
    }

    @Test
    public void getRevocationEndpoint() {
        String endpoint = mConfig.getRevocationEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + REVOCATION_ENDPOINT, endpoint);
    }

    @Test
    public void getEndSessionEndpoint() {
        String endpoint = mConfig.getEndSessionEndpoint();
        assertNotNull(endpoint);
        assertEquals(CUSTOM_URL + END_SESSION_ENDPOINT, endpoint);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidConfigurationNoTokenEndpoint() {
        CustomConfiguration config = new CustomConfiguration.Builder()
                .authorizationEndpoint(CUSTOM_URL + AUTHORIZATION_ENDPOINT)
                .create();
    }

    @Test(expected = IllegalStateException.class)
    public void invalidConfigurationNoAuthorizationEndpoint() {
        CustomConfiguration config = new CustomConfiguration.Builder()
                .tokenEndpoint(CUSTOM_URL + TOKEN_ENDPOINT)
                .create();
    }
}