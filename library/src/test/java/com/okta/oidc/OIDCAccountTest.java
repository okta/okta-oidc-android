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
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.JsonStrings.VALID_ID_TOKEN;
import static com.okta.oidc.util.TestValues.CLIENT_ID;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static com.okta.oidc.util.TestValues.END_SESSION_URI;
import static com.okta.oidc.util.TestValues.REDIRECT_URI;
import static com.okta.oidc.util.TestValues.SCOPES;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OIDCAccountTest {
    private OIDCAccount mAccount;

    @Before
    public void setUp() throws Exception {
        mAccount = TestValues.getAccountWithUrl(CUSTOM_URL);
    }

    @Test
    public void getClientId() {
        String id = mAccount.getClientId();
        assertNotNull(id);
        assertEquals(CLIENT_ID, id);
    }

    @Test
    public void getRedirectUri() {
        Uri uri = mAccount.getRedirectUri();
        assertNotNull(uri);
        assertEquals(uri, Uri.parse(REDIRECT_URI));
    }

    @Test
    public void getEndSessionRedirectUri() {
        Uri uri = mAccount.getEndSessionRedirectUri();
        assertNotNull(uri);
        assertEquals(uri, Uri.parse(END_SESSION_URI));
    }

    @Test
    public void getDiscoveryUri() {
        Uri uri = mAccount.getDiscoveryUri();
        assertNotNull(uri);
        assertEquals(uri, Uri.parse(CUSTOM_URL +
                ProviderConfiguration.OPENID_CONFIGURATION_RESOURCE));
    }

    @Test
    public void getScopes() {
        String[] scopes = mAccount.getScopes();
        assertNotNull(scopes);
        assertArrayEquals(SCOPES, scopes);
    }

    @Test
    public void testBuilder() {
        OIDCAccount.Builder builder = mock(OIDCAccount.Builder.class);
        OIDCAccount otherAccount = TestValues.getAccountWithUrl(CUSTOM_URL);
        when(builder.create()).thenReturn(otherAccount);

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
        OIDCAccount account = builder.create();
        assertEquals(mAccount.getClientId(), account.getClientId());
        assertEquals(mAccount.getDiscoveryUri(), account.getDiscoveryUri());
        assertEquals(mAccount.getEndSessionRedirectUri(), account.getEndSessionRedirectUri());
        assertEquals(mAccount.getRedirectUri(), account.getRedirectUri());
        assertArrayEquals(mAccount.getScopes(), account.getScopes());
    }
}