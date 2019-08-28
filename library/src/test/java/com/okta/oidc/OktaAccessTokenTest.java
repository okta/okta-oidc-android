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

import com.okta.oidc.util.JsonStrings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)

public class OktaAccessTokenTest {
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Test
    public void parseValidAccessToken() {
        OktaAccessToken token = OktaAccessToken.parseAccessToken(JsonStrings.VALID_ACCESS_TOKEN);
        assertNotNull(token.mPayload);
        assertNotNull(token.mSignature);
        assertNotNull(token.mHeader);
        assertEquals("RS256", token.mHeader.alg);
    }

    @Test
    public void parseInvalidAccessToken() {
        mExpectedEx.expect(IllegalArgumentException.class);
        //can reuse the invalid id token to test.
        OktaAccessToken.parseAccessToken(JsonStrings.INVALID_ID_TOKEN);
    }
}