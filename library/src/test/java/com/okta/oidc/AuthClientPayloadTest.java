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
import org.robolectric.util.Pair;

import java.util.Map;

import static com.okta.oidc.net.request.web.AuthorizeRequest.IDP;
import static com.okta.oidc.net.request.web.AuthorizeRequest.IDP_SCOPE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.LOGIN_HINT;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthClientPayloadTest {
    private AuthenticationPayload mAuthenticationPayload;
    private String PARAMETER_KEY = "PARAMETER_KEY";
    private String PARAMETER_VALUE = "PARAMETER_VALUE";

    @Before
    public void setUp() {
        mAuthenticationPayload = TestValues.getAuthenticationPayload(new Pair<>(PARAMETER_KEY, PARAMETER_VALUE));
    }

    @Test
    public void getLoginHint() {
        String loginHint = mAuthenticationPayload.getLoginHint();
        assertNotNull(loginHint);
        assertEquals(LOGIN_HINT, loginHint);
    }

    @Test
    public void getState() {
        String state = mAuthenticationPayload.getState();
        assertNotNull(state);
        assertEquals(state, CUSTOM_STATE);
    }

    @Test
    public void setNullState() {
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .setState(null)
                .build();
        assertNull(payload.getState());
    }

    @Test
    public void setNullHint() {
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .setLoginHint(null)
                .build();
        assertNull(payload.getLoginHint());
    }

    @Test
    public void setEmptyState() {
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .setState("")
                .build();
        assertNull(payload.getState());
    }

    @Test
    public void setEmptyHint() {
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .setLoginHint("")
                .build();
        assertNull(payload.getLoginHint());
    }

    @Test
    public void getAdditionalParameters() {
        Map<String, String> additionalParameters = mAuthenticationPayload.getAdditionalParameters();
        assertNotNull(additionalParameters);
        assert (additionalParameters.size() > 0);
        assertEquals(PARAMETER_VALUE, additionalParameters.get(PARAMETER_KEY));
    }

    @Test
    public void testBuilder() {
        AuthenticationPayload.Builder builder = mock(AuthenticationPayload.Builder.class);
        AuthenticationPayload authenticationPayload = TestValues.getAuthenticationPayload(new Pair<>(PARAMETER_KEY, PARAMETER_VALUE));
        when(builder.build()).thenReturn(authenticationPayload);

        builder.setLoginHint(LOGIN_HINT);
        verify(builder).setLoginHint(LOGIN_HINT);

        builder.setState(CUSTOM_STATE);
        verify(builder).setState(CUSTOM_STATE);

        builder.addParameter(PARAMETER_KEY, PARAMETER_VALUE);
        verify(builder).addParameter(PARAMETER_KEY, PARAMETER_VALUE);

        AuthenticationPayload payload = builder.build();
        assertEquals(authenticationPayload.getLoginHint(), payload.getLoginHint());
        assertEquals(authenticationPayload.getState(), payload.getState());
        assertArrayEquals(authenticationPayload.getAdditionalParameters().keySet().toArray(), payload.getAdditionalParameters().keySet().toArray());
        assertArrayEquals(authenticationPayload.getAdditionalParameters().values().toArray(), payload.getAdditionalParameters().values().toArray());
    }

    @Test
    public void setIdp() {
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .setIdp("MyIDP")
                .build();
        assertEquals("MyIDP", payload.getAdditionalParameters().get(IDP));
    }

    @Test
    public void setIdpScope() {
        AuthenticationPayload payload = new AuthenticationPayload.Builder()
                .setIdpScope("email", "openid", "profile")
                .build();
        assertEquals("email openid profile", payload.getAdditionalParameters().get(IDP_SCOPE));
    }

    @Test
    public void addPayload() {
        AuthenticationPayload payload = TestValues.getAuthenticationPayload(new Pair<>(PARAMETER_KEY, PARAMETER_VALUE));

        AuthenticationPayload copy = new AuthenticationPayload.Builder().copyPayload(payload)
                .build();

        assertEquals(payload.getLoginHint(), copy.getLoginHint());
        assertEquals(payload.getState(), copy.getState());
        assertEquals(payload.getAdditionalParameters(), copy.getAdditionalParameters());

        AuthenticationPayload copyWithNewHint = new AuthenticationPayload.Builder().copyPayload(payload)
                .setLoginHint("NewHint")
                .build();
        assertNotEquals(payload.getLoginHint(), copyWithNewHint.getLoginHint());
    }
}
