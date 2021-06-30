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

package com.okta.oidc.example;

import android.content.Context;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.okta.oidc.OktaBuilder;
import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.DefaultEncryptionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EncryptionTest {
    private EncryptionManager mEncryptionManager;
    private String mConfiguration;
    @Rule
    public ActivityTestRule<SampleActivity> activityRule = new ActivityTestRule<>(SampleActivity.class);

    @Before
    public void setUp() {
        mEncryptionManager = new DefaultEncryptionManager(activityRule.getActivity());
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mConfiguration = Utils.getAsset(context, "configuration.json");
    }

    @Test
    public void checkHardwareKeystore() {
        boolean hardwareBacked = mEncryptionManager.isHardwareBackedKeyStore();
        if (OktaBuilder.isEmulator()) {
            //API 26, isHardwareBackedKeyStore on emulator return true
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
                assertFalse(hardwareBacked);
            } else {
                assertTrue(hardwareBacked);
            }
        } else {
            assertTrue(hardwareBacked);
        }
    }

    @Test
    public void encryptData() throws GeneralSecurityException, IOException,
            IllegalArgumentException {
        String encrypted = mEncryptionManager.encrypt(mConfiguration);
        String decryptData = mEncryptionManager.decrypt(encrypted);
        assertNotNull(encrypted);
        assertNotNull(mConfiguration);
        assertNotEquals(encrypted, mConfiguration);
        assertNotEquals(encrypted, decryptData);
    }

    @Test
    public void decryptData() throws GeneralSecurityException, IOException,
            IllegalArgumentException {
        String encrypted = mEncryptionManager.encrypt(mConfiguration);
        String decryptData = mEncryptionManager.decrypt(encrypted);
        assertNotNull(decryptData);
        assertNotNull(mConfiguration);
        assertEquals(decryptData, mConfiguration);
    }

    @Test
    public void getHash() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String testValue = "TestValue";
        String hashedValue = mEncryptionManager.getHashed(testValue);
        assertNotEquals(hashedValue, testValue);

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] result = digest.digest(testValue.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02X", b));
        }
        String sameHash = sb.toString();
        assertNotNull(sameHash);
        assertNotNull(hashedValue);
        assertEquals(sameHash, hashedValue);
    }

}
