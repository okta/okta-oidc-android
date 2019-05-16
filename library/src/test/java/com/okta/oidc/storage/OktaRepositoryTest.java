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

package com.okta.oidc.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.okta.oidc.util.EncryptedPersistableMock;
import com.okta.oidc.util.EncryptionManagerStub;
import com.okta.oidc.util.OktaStorageMock;
import com.okta.oidc.util.PersistableMock;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaRepositoryTest {
    private static final String PREF_NAME_HARDWARE = "HARDWARE_REQUIREMENT";
    private static final String PREF_NAME_SOFTWARE = "NO_HARDWARE_REQUIREMENT";

    //encryption manager that has hardware support
    private EncryptionManagerStub mHardwareEncryption;
    //encryption manager that does not have hardware support
    private EncryptionManagerStub mSoftwareEncryption;

    //storage that requires hardware support
    private OktaStorageMock mOktaStorageHardware;
    //storage that does not require hardware support
    private OktaStorageMock mOktaStorageSoftware;

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mHardwareEncryption = new EncryptionManagerStub(true);
        mSoftwareEncryption = new EncryptionManagerStub(false);
        mOktaStorageSoftware = new OktaStorageMock(mContext, PREF_NAME_SOFTWARE, false);
        mOktaStorageHardware = new OktaStorageMock(mContext, PREF_NAME_HARDWARE, true);
    }

    @Test
    public void saveEncryptedItemSuccess() {
        OktaRepository repository = new OktaRepository(mOktaStorageSoftware, mContext,
                mSoftwareEncryption);
        EncryptedPersistableMock encrypted = TestValues.getEncryptedPersistable();
        repository.save(encrypted);

        EncryptedPersistableMock savedItem = repository
                .get(EncryptedPersistableMock.RESTORE);
        assert (savedItem != null);
        assert (mOktaStorageSoftware.get(encrypted.getKey()) == null);
        assert (repository.cacheStorage.get(encrypted.getKey()) == null);
        //TODO: SimpleEncryptionManager use AndroidKeyStore. Robolectric doesn't support it. Follow asserts check if saved data is encrypted
        //assert(!mOktaStorageMock.get(mOktaRepository.getHashed(encrypted.getKey())).equalsIgnoreCase(encrypted.getData()));
        //assert(!mOktaRepository.cacheStorage.get(mOktaRepository.getHashed(encrypted.getKey())).equalsIgnoreCase(encrypted.getData()));

        assert (encrypted.getData().equalsIgnoreCase(savedItem.getData()));
    }

    @Test
    public void removeItemSuccess() {
        PersistableMock notEncrypted = TestValues.getNotEncryptedPersistable();
        OktaRepository repository = new OktaRepository(mOktaStorageSoftware, mContext,
                mSoftwareEncryption);
        repository.save(notEncrypted);
        repository.delete(notEncrypted);

        assert (repository.get(PersistableMock.RESTORE) == null);
        assert (repository.cacheStorage.get(PersistableMock.RESTORE.getKey()) == null);
    }

    @Test //should encrypt data and stored to device
    public void noHwRequiredAndNotSupported() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        OktaRepository repository = new OktaRepository(mOktaStorageSoftware, mContext,
                mSoftwareEncryption);
        PersistableMock persistable = TestValues.getNotEncryptedPersistable();
        repository.save(persistable);

        String hashedKey = mSoftwareEncryption.getHashed(persistable.getKey());
        String encryptedValue = mOktaStorageSoftware.getSharedPreferences()
                .getString(hashedKey, null);
        assertNotNull(encryptedValue);
        assertNotEquals(encryptedValue, persistable.getData());
        assertEquals(encryptedValue, persistable.getData() +
                EncryptionManagerStub.STUPID_SALT);
    }

    @Test //should encrypt data and stored to device
    public void noHwRequiredAndSupported() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        OktaRepository repository = new OktaRepository(mOktaStorageSoftware, mContext,
                mHardwareEncryption);
        PersistableMock persistable = TestValues.getNotEncryptedPersistable();
        repository.save(persistable);

        String hashedKey = mHardwareEncryption.getHashed(persistable.getKey());
        String encryptedValue = mOktaStorageSoftware.getSharedPreferences()
                .getString(hashedKey, null);
        assertNotNull(encryptedValue);
        assertNotEquals(encryptedValue, persistable.getData());
        assertEquals(encryptedValue, persistable.getData() +
                EncryptionManagerStub.STUPID_SALT);
    }

    @Test //should encrypt data and not store on device.
    public void hwRequiredAndNotSupported() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        OktaRepository repository = new OktaRepository(mOktaStorageHardware, mContext,
                mSoftwareEncryption);
        PersistableMock persistable = TestValues.getNotEncryptedPersistable();
        repository.save(persistable);

        String hashedKey = mSoftwareEncryption.getHashed(persistable.getKey());
        String encryptedValue = mOktaStorageHardware.getSharedPreferences()
                .getString(hashedKey, null);

        assertNull(encryptedValue);

        PersistableMock encryptedFromCache = repository.get(PersistableMock.RESTORE);
        String valueFromCache = encryptedFromCache.getData();

        assertEquals(valueFromCache, persistable.getData());
        assertNotEquals(valueFromCache, persistable.getData() +
                EncryptionManagerStub.STUPID_SALT);
    }

    @Test //should encrypt data and store on device.
    public void hwRequiredAndSupported() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        OktaRepository repository = new OktaRepository(mOktaStorageHardware, mContext,
                mHardwareEncryption);
        PersistableMock persistable = TestValues.getNotEncryptedPersistable();
        repository.save(persistable);

        String hashedKey = mHardwareEncryption.getHashed(persistable.getKey());
        String encryptedValue = mOktaStorageHardware.getSharedPreferences()
                .getString(hashedKey, null);

        assertNotNull(encryptedValue);
        assertEquals(encryptedValue, persistable.getData() +
                EncryptionManagerStub.STUPID_SALT);

        PersistableMock encryptedFromCache = repository.get(PersistableMock.RESTORE);
        String valueFromCache = encryptedFromCache.getData();

        assertEquals(valueFromCache, persistable.getData());
        assertNotEquals(valueFromCache, persistable.getData() +
                EncryptionManagerStub.STUPID_SALT);
    }
}
