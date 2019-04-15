package com.okta.oidc;

import android.content.Context;

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.util.OktaStorageMock;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.platform.app.InstrumentationRegistry;

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
        mOktaStorageMock = new OktaStorageMock();
        mOktaRepository = new OktaRepository(mOktaStorageMock, mContext);
        mOktaState = new OktaState(mOktaRepository);
    }

    @Test
    public void getAuthorizeRequest() {
        WebRequest authorizedRequest = TestValues.getAuthorizeRequest(TestValues.getAccountWithUrl(CUSTOM_URL), null);
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
