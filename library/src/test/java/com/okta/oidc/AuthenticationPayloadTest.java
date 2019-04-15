package com.okta.oidc;

import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.Pair;

import java.util.Map;

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.LOGIN_HINT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthenticationPayloadTest {
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
}
