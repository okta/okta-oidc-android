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
package com.okta.oidc.net.request.web;

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.response.TokenResponse;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class LogoutRequestTest {
    private LogoutRequest mRequest;
    private OIDCConfig mConfig;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();
    private TokenResponse mTokenResponse;
    private ProviderConfiguration mConfiguration;

    @Before
    public void setUp() {
        mConfig = TestValues.getConfigWithUrl(CUSTOM_URL);
        mConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mTokenResponse = TokenResponse.RESTORE.restore(TOKEN_RESPONSE);

        mRequest = new LogoutRequest.Builder()
                .provideConfiguration(mConfiguration)
                .tokenResponse(mTokenResponse)
                .state(CUSTOM_STATE)
                .config(mConfig)
                .create();
    }

    @Test
    public void testBuilderFailEndpointMissing() {
        LogoutRequest.Builder builder = new LogoutRequest.Builder();
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("end_session_endpoint missing");
        builder.create();
    }

    @Test
    public void testBuilderFailTokenMissing() {
        LogoutRequest.Builder builder = new LogoutRequest.Builder();
        builder.endSessionEndpoint(mConfig.getEndSessionRedirectUri().toString());
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("id_token_hint missing");
        builder.create();
    }

    @Test
    public void testBuilderFailRedirectMissing() {
        LogoutRequest.Builder builder = new LogoutRequest.Builder();
        builder.endSessionEndpoint(mConfig.getEndSessionRedirectUri().toString());
        builder.idTokenHint(mTokenResponse.getIdToken());
        mExpectedEx.expect(IllegalArgumentException.class);
        mExpectedEx.expectMessage("post_logout_redirect_uri missing");
        builder.create();
    }

    @Test
    public void testBuilder() {
        LogoutRequest request = new LogoutRequest.Builder()
                .state(CUSTOM_STATE)
                .config(mConfig)
                .tokenResponse(mTokenResponse)
                .provideConfiguration(mConfiguration)
                .create();
        assertEquals(mRequest.persist(), request.persist());
    }

    @Test
    public void getState() {
        assertEquals(mRequest.getState(), CUSTOM_STATE);
    }

    @Test
    public void toUri() {
        Uri uri = mRequest.toUri();
        assertEquals(uri.getQueryParameter("id_token_hint"), mTokenResponse.getIdToken());
        assertEquals(uri.getQueryParameter("state"), CUSTOM_STATE);
        assertEquals(uri.getQueryParameter("post_logout_redirect_uri"),
                mConfig.getEndSessionRedirectUri().toString());
    }

    @Test
    public void getKey() {
        assertEquals(mRequest.getKey(), "WebRequest");
    }

    @Test
    public void persist() {
        String json = mRequest.persist();
        LogoutRequest.Parameters parameters = new Gson().fromJson(json, LogoutRequest.Parameters.class);
        LogoutRequest request = new LogoutRequest(parameters);
        assertEquals(request.persist(), json);
    }
}