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
import com.okta.oidc.util.OktaStorageMock;
import com.okta.oidc.util.PersistableMock;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaRepositoryTest {
    OktaRepository mOktaRepository;
    OktaStorageMock mOktaStorageMock;
    Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mOktaStorageMock = new OktaStorageMock();
        mOktaRepository = new OktaRepository(mOktaStorageMock, mContext);
    }

    @Test
    public void saveEncryptedItemSuccess() {
        EncryptedPersistableMock encrypted = TestValues.getEncryptedPersistable();
        mOktaRepository.save(encrypted);

        EncryptedPersistableMock savedItem = mOktaRepository.get(EncryptedPersistableMock.RESTORE);
        assert(savedItem != null);

        assert(mOktaStorageMock.get(encrypted.getKey()) == null);
        assert(mOktaRepository.cacheStorage.get(encrypted.getKey()) == null);
        //TODO: EncryptionManager use AndroidKeyStore. Robolectric doesn't support it. Follow asserts check if saved data is encrypted
        //assert(!mOktaStorageMock.get(mOktaRepository.getHashed(encrypted.getKey())).equalsIgnoreCase(encrypted.getData()));
        //assert(!mOktaRepository.cacheStorage.get(mOktaRepository.getHashed(encrypted.getKey())).equalsIgnoreCase(encrypted.getData()));

        assert(encrypted.getData().equalsIgnoreCase(savedItem.getData()));
    }

    @Test
    public void removeItemSuccess() {
        PersistableMock notEncrypted = TestValues.getNotEncryptedPersistable();
        mOktaRepository.save(notEncrypted);
        mOktaRepository.delete(notEncrypted);

        assert(mOktaRepository.get(PersistableMock.RESTORE) == null);
        assert(mOktaRepository.cacheStorage.get(PersistableMock.RESTORE.getKey()) == null);
    }
}
