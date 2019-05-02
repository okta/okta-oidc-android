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

import android.net.Uri;

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.CLIENT_ID;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.END_SESSION_URI;
import static com.okta.oidc.util.TestValues.REDIRECT_URI;
import static com.okta.oidc.util.TestValues.SCOPES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OIDCConfigTest {
    private OIDCConfig mConfig;

    @Before
    public void setUp() throws Exception {
        mConfig = TestValues.getConfigWithUrl(CUSTOM_URL);
    }

    @Test
    public void getClientId() {
        String id = mConfig.getClientId();
        assertNotNull(id);
        assertEquals(CLIENT_ID, id);
    }

    @Test
    public void getRedirectUri() {
        Uri uri = mConfig.getRedirectUri();
        assertNotNull(uri);
        assertEquals(uri, Uri.parse(REDIRECT_URI));
    }

    @Test
    public void getEndSessionRedirectUri() {
        Uri uri = mConfig.getEndSessionRedirectUri();
        assertNotNull(uri);
        assertEquals(uri, Uri.parse(END_SESSION_URI));
    }

    @Test
    public void getDiscoveryUri() {
        Uri uri = mConfig.getDiscoveryUri();
        assertNotNull(uri);
        assertEquals(uri, Uri.parse(CUSTOM_URL +
                ProviderConfiguration.OPENID_CONFIGURATION_RESOURCE));
    }

    @Test
    public void getScopes() {
        String[] scopes = mConfig.getScopes();
        assertNotNull(scopes);
        assertArrayEquals(SCOPES, scopes);
    }

    @Test
    public void testBuilder() {
        OIDCConfig.Builder builder = mock(OIDCConfig.Builder.class);
        OIDCConfig otherConfig = TestValues.getConfigWithUrl(CUSTOM_URL);
        when(builder.create()).thenReturn(otherConfig);

        builder.clientId(CLIENT_ID);
        verify(builder).clientId(CLIENT_ID);

        builder.redirectUri(REDIRECT_URI);
        verify(builder).redirectUri(REDIRECT_URI);

        builder.endSessionRedirectUri(END_SESSION_URI);
        verify(builder).endSessionRedirectUri(END_SESSION_URI);

        builder.scopes(SCOPES);
        verify(builder).scopes(SCOPES);

        builder.discoveryUri(CUSTOM_URL);
        verify(builder).discoveryUri(CUSTOM_URL);
        OIDCConfig config = builder.create();
        assertEquals(mConfig.getClientId(), config.getClientId());
        assertEquals(mConfig.getDiscoveryUri(), config.getDiscoveryUri());
        assertEquals(mConfig.getEndSessionRedirectUri(), config.getEndSessionRedirectUri());
        assertEquals(mConfig.getRedirectUri(), config.getRedirectUri());
        assertArrayEquals(mConfig.getScopes(), config.getScopes());
    }
}