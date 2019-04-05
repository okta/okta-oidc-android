package com.okta.oidc;

import android.text.TextUtils;

import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.ACCESS_TOKEN;
import static com.okta.oidc.util.TestValues.ID_TOKEN;
import static com.okta.oidc.util.TestValues.REFRESH_TOKEN;
import static com.okta.oidc.util.TestValues.VALID_EXPIRES_IN;
import static com.okta.oidc.util.TestValues.VALID_SCOPES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class TokensTest {
    private String INVALID_EXPIRES_IN = "INVALID_EXPIRES_IN";

    private String invalidTokenResponsePayload = TestValues.generatePayloadTokenResponse(
            TestValues.ACCESS_TOKEN,
            TestValues.ID_TOKEN,
            TestValues.REFRESH_TOKEN,
            INVALID_EXPIRES_IN,
            TextUtils.join(" ", VALID_SCOPES));

    @Test
    public void validateTokenInit() {
        Tokens tokens = new Tokens(ID_TOKEN,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                VALID_EXPIRES_IN,
                VALID_SCOPES);

        assertNotNull(tokens);
        assertEquals(ACCESS_TOKEN, tokens.getAccessToken());
        assertEquals(REFRESH_TOKEN, tokens.getRefreshToken());
        assertEquals(ID_TOKEN, tokens.getIdToken());
        assertEquals(VALID_EXPIRES_IN, tokens.getExpiresIn());
        assertArrayEquals(VALID_SCOPES, tokens.getScope());
    }

    @Test
    public void validateTokenInit_fromTokenResponse() {
        TokenResponse tokenResponse = TestValues.getTokenResponse();
        Tokens tokens = new Tokens(tokenResponse);

        assertNotNull(tokens);
        assertEquals(ACCESS_TOKEN, tokens.getAccessToken());
        assertEquals(REFRESH_TOKEN, tokens.getRefreshToken());
        assertEquals(ID_TOKEN, tokens.getIdToken());
        assertEquals(VALID_EXPIRES_IN, tokens.getExpiresIn());
        assertArrayEquals(VALID_SCOPES, tokens.getScope());
    }

    @Test(expected = NumberFormatException.class)
    public void validateTokenInit_fromInvalidTokenResponse() {
        TokenResponse tokenResponse = TokenResponse.RESTORE.restore(invalidTokenResponsePayload);
        new Tokens(tokenResponse);
    }

}
