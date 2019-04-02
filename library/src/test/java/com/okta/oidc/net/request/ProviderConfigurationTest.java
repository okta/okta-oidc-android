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
package com.okta.oidc.net.request;

import com.google.gson.Gson;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ProviderConfigurationTest {
    private ProviderConfiguration mValidConfiguration;

    private ProviderConfiguration mInvalidConfiguration;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mValidConfiguration = TestValues.getProviderConfiguration(CUSTOM_URL);
        mInvalidConfiguration = TestValues.getProviderConfiguration(null);
    }

    @Test
    public void validate() throws IllegalArgumentException {
        mValidConfiguration.validate();
    }

    @Test
    public void validateFail() throws IllegalArgumentException {
        mExpectedEx.expect(IllegalArgumentException.class);
        mInvalidConfiguration.validate();
    }

    @Test
    public void encrypt() {
        assertFalse(mValidConfiguration.encrypt());
    }


    @Test
    public void getKey() {
        assertEquals(mValidConfiguration.getKey(), ProviderConfiguration.RESTORE.getKey());
    }

    @Test
    public void persist() {
        String json = mValidConfiguration.persist();
        ProviderConfiguration other = new Gson().fromJson(json, ProviderConfiguration.class);
        other.validate();
        assertNotNull(other);
        assertEquals(other.persist(), json);
    }
}