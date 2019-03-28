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

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.net.response.TokenResponse.RESTORE;
import static com.okta.oidc.util.JsonStrings.TOKEN_RESPONSE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class WebResponseTest {
    private WebResponse mWebResponseImpl;

    @Before
    public void setUp() throws Exception {
        mWebResponseImpl = new WebResponse() {
            private String mState = CUSTOM_STATE;
            private static final String KEY = "WebResponseImpl";

            @Override
            public String getState() {
                return mState;
            }

            @NonNull
            @Override
            public String getKey() {
                return KEY;
            }

            @Override
            public String persist() {
                return mState;
            }

            @Override
            public boolean encrypt() {
                return false;
            }
        };
    }

    @Test
    public void getState() {
        assertEquals(mWebResponseImpl.getState(), CUSTOM_STATE);
    }

    @Test
    public void getKey() {
        assertEquals(mWebResponseImpl.getKey(), "WebResponseImpl");
    }

    @Test
    public void persist() {
        assertEquals(mWebResponseImpl.persist(), CUSTOM_STATE);
    }

    @Test
    public void encrypt() {
        assertFalse(mWebResponseImpl.encrypt());
    }
}