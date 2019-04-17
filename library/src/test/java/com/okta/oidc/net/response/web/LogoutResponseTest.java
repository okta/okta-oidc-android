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

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class LogoutResponseTest {
    private LogoutResponse mResponse;

    @Before
    public void setUp() {
        mResponse = TestValues.getLogoutResponse(CUSTOM_STATE);
    }

    @Test
    public void fromUri() {
        String uri = String.format("com.okta.test:/logout?state=%s", CUSTOM_STATE);
        LogoutResponse response = LogoutResponse.fromUri(Uri.parse(uri));
        assertEquals(response.getState(), mResponse.getState());
    }

    @Test
    public void getState() {
        assertEquals(mResponse.getState(), CUSTOM_STATE);
    }

    @Test
    public void getKey() {
        assertEquals(mResponse.getKey(), WebResponse.RESTORE.getKey());
    }

    @Test
    public void persist() {
        String json = mResponse.persist();
        LogoutResponse logoutResponse = new Gson().fromJson(json, LogoutResponse.class);
        assertEquals(logoutResponse.getState(), mResponse.getState());
    }

}