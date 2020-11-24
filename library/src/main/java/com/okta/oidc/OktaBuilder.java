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

import android.content.Context;

import androidx.annotation.NonNull;

import com.okta.oidc.clients.ClientFactory;
import com.okta.oidc.net.HttpClientImpl;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.storage.security.DefaultEncryptionManager;
import com.okta.oidc.storage.security.EncryptionManager;

/**
 * The base type Okta builder.
 *
 * @param <A> the generic type for the auth client
 * @param <T> the generic type for the auth client builder.
 */

public abstract class OktaBuilder<A, T extends OktaBuilder<A, T>> {
    /**
     * The connection factory.
     */
    private OktaHttpClient mClient;
    /**
     * The oidc config.
     */
    private OIDCConfig mOidcConfig;
    /**
     * The storage.
     */
    private OktaStorage mStorage;
    /**
     * The Context.
     */
    private Context mContext;
    /**
     * The Auth client factory.
     */
    private ClientFactory<A> mClientFactory;

    /**
     * The Encryption Manager.
     */
    private EncryptionManager mEncryptionManager;

    /**
     * Require Hardware Backed KeyStore.
     */
    private boolean mRequireHardwareBackedKeyStore = true;

    /**
     * Cache Mode.
     */
    private boolean mCacheMode = true;

    /**
     * Used to prevent lint issues.
     *
     * @return the generic of the client builder
     */
    protected abstract T toThis();

    /**
     * Create a.
     *
     * @return the a
     */
    protected abstract A create();

    /**
     * Sets the config used for this client.
     * {@link OIDCConfig}
     *
     * @param config the config
     * @return current builder
     */
    public T withConfig(@NonNull OIDCConfig config) {
        mOidcConfig = config;
        return toThis();
    }

    /**
     * Sets the OktaHttpClient to use {@link OktaHttpClient}.
     *
     * @param client the OktaHttpClient
     * @return current builder
     */
    public T withOktaHttpClient(OktaHttpClient client) {
        mClient = client;
        return toThis();
    }

    /**
     * Sets the context.
     *
     * @param context the context
     * @return current builder
     */
    public T withContext(Context context) {
        mContext = context;
        return toThis();
    }

    /**
     * Set a storage implementation for the client to use. You can define your own storage
     * or use the default implementation {@link SharedPreferenceStorage}
     *
     * @param storage the storage implementation
     * @return current builder
     */
    public T withStorage(OktaStorage storage) {
        this.mStorage = storage;
        return toThis();
    }

    /**
     * With authentication client factory t.
     *
     * @param clientFactory the auth client factory
     * @return current builder
     */
    protected T withAuthenticationClientFactory(ClientFactory<A> clientFactory) {
        mClientFactory = clientFactory;
        return toThis();
    }

    /**
     * Sets specific implementation of encryption manager that will be used for
     * encrypting data that is stored locally by oidc.
     *
     * @param encryptionManager manager for encryption of locally stored data
     * @return current builder
     */
    public T withEncryptionManager(EncryptionManager encryptionManager) {
        mEncryptionManager = encryptionManager;
        return toThis();
    }

    /**
     * Sets if hardware backed keystore is required for storing data. If true and the device
     * doesn't have hardware support then no data will be persisted on the device.
     *
     * @return true if hardware backed keystore is required.
     */
    public T setRequireHardwareBackedKeyStore(boolean requireHardwareBackedKeyStore) {
        mRequireHardwareBackedKeyStore = requireHardwareBackedKeyStore;
        return toThis();
    }

    /**
     * Sets if hardware backed keystore is required for storing data. If true and the device
     * doesn't have hardware support then no data will be persisted on the device.
     *
     * @return true if hardware backed keystore is required.
     */
    public T setCacheMode(boolean cacheMode) {
        mCacheMode = cacheMode;
        return toThis();
    }

    /**
     * Create auth client.
     *
     * @return the a AuthClient
     */
    @SuppressWarnings("WeakerAccess")
    protected A createAuthClient() {
        if (mClient == null) {
            mClient = new HttpClientImpl();
        }
        // By default we enable encryption for all our clients. To change this behaviour, create
        // your own Builder.
        if (mEncryptionManager == null) {
            mEncryptionManager = new DefaultEncryptionManager(mContext);
        }
        if (mStorage == null) {
            mStorage = new SharedPreferenceStorage(mContext);
        }
        return this.mClientFactory.createClient(mOidcConfig,
                mContext, mStorage, mEncryptionManager,
                mClient, mRequireHardwareBackedKeyStore, mCacheMode);
    }
}
