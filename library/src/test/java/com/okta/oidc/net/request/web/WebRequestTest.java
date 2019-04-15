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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class WebRequestTest {
    private WebRequest mWebRequestImpl;
    private static final String KEY = "mWebRequestImpl";

    @Before
    public void setUp() throws Exception {
        mWebRequestImpl = new WebRequest() {
            private String mState = CUSTOM_STATE;

            @NonNull
            @Override
            public Uri toUri() {
                return Uri.parse(CUSTOM_URL).buildUpon()
                        .appendQueryParameter("state", mState).build();
            }

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
    public void toUri() {
        assertEquals(mWebRequestImpl.toUri().getQueryParameter("state"), CUSTOM_STATE);
    }

    @Test
    public void getState() {
        assertEquals(mWebRequestImpl.getState(), CUSTOM_STATE);
    }

    @Test
    public void getKey() {
        assertEquals(mWebRequestImpl.getKey(), KEY);
    }

    @Test
    public void persist() {
        assertEquals(mWebRequestImpl.persist(), CUSTOM_STATE);
    }

    @Test
    public void encrypt() {
        assertFalse(mWebRequestImpl.encrypt());
    }
}