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
package com.okta.oidc.net.response.web;

import android.net.Uri;

import com.google.gson.Gson;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.net.response.web.WebResponse.RESTORE;
import static com.okta.oidc.util.TestValues.CUSTOM_CODE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.ERROR;
import static com.okta.oidc.util.TestValues.ERROR_DESCRIPTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthorizeResponseTest {
    private AuthorizeResponse mResponse;
    private AuthorizeResponse mInvalidResponse;

    @Before
    public void setUp() {
        mResponse = TestValues.getAuthorizeResponse(CUSTOM_STATE, CUSTOM_CODE);
        mInvalidResponse = TestValues.getInvalidAuthorizeResponse(ERROR, ERROR_DESCRIPTION);
    }

    @Test
    public void fromUri() {
        String uri = String.format("com.okta.test:/callback?code=%s&state=%s",
                CUSTOM_CODE, CUSTOM_STATE);
        AuthorizeResponse response = AuthorizeResponse.fromUri(Uri.parse(uri));
        assertEquals(response.persist(), mResponse.persist());
    }

    @Test
    public void getState() {
        assertEquals(mResponse.getState(), CUSTOM_STATE);
    }

    @Test
    public void getCode() {
        assertEquals(mResponse.getCode(), CUSTOM_CODE);
    }

    @Test
    public void getError() {
        assertEquals(mInvalidResponse.getError(), ERROR);
    }

    @Test
    public void getErrorDescription() {
        assertEquals(mInvalidResponse.getErrorDescription(), ERROR_DESCRIPTION);
    }

    @Test
    public void getKey() {
        assertEquals(mResponse.getKey(), RESTORE.getKey());
    }

    @Test
    public void persist() {
        String json = mResponse.persist();
        AuthorizeResponse authorizeResponse = new Gson().fromJson(json, AuthorizeResponse.class);
        assertEquals(json, authorizeResponse.persist());
    }

    @Test
    public void equals() {
        assertNotEquals(mResponse, mInvalidResponse);
    }
}