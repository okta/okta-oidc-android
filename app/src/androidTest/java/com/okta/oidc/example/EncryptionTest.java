package com.okta.oidc.example;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.storage.security.SimpleEncryptionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.NoSuchPaddingException;

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
    public void setUp() throws IOException, CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, UnrecoverableEntryException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, NoSuchProviderException, KeyStoreException {
        mEncryptionManager = new SimpleEncryptionManager(activityRule.getActivity());
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mConfiguration = Utils.getAsset(context, "configuration.json");
    }

    @Test
    public void checkHardwareKeystore() {
        boolean hardwareBacked = mEncryptionManager.isHardwareBackedKeyStore();
        if (Utils.isEmulator()) {
            assertFalse(hardwareBacked);
        } else {
            assertTrue(hardwareBacked);
        }
    }

    @Test
    public void encryptData() throws GeneralSecurityException, IOException,
            IllegalArgumentException {
        String encrypted = mEncryptionManager.encrypt(mConfiguration);
        //check if the encrypted data is base64 encoded.
        android.util.Base64.decode(encrypted, android.util.Base64.NO_WRAP);
        assertNotNull(encrypted);
        assertNotNull(mConfiguration);
        assertNotEquals(encrypted, mConfiguration);
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
