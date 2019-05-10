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
import androidx.annotation.RestrictTo;

import com.okta.oidc.clients.ClientFactory;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.OktaRepository;
import com.okta.oidc.storage.OktaStorage;
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
    private HttpConnectionFactory mConnectionFactory;
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
     * Gets connection factory.
     *
     * @return the connection factory
     */
    public HttpConnectionFactory getConnectionFactory() {
        return mConnectionFactory;
    }

    /**
     * Gets oidc config.
     *
     * @return the oidc config
     */
    public OIDCConfig getOidcConfig() {
        return mOidcConfig;
    }

    /**
     * Gets storage.
     *
     * @return the storage
     */
    public OktaStorage getStorage() {
        return mStorage;
    }

    /**
     * Gets context.
     *
     * @return the context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Gets encryption manager.
     *
     * @return the encryption manager
     */
    public EncryptionManager getEncryptionManager() {
        return mEncryptionManager;
    }

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
     * Sets the connection factory to use, which creates a {@link java.net.HttpURLConnection}
     * instance for communication with Okta OIDC endpoints.
     *
     * @param connectionFactory the connection factory
     * @return current builder
     */
    public T withHttpConnectionFactory(HttpConnectionFactory connectionFactory) {
        mConnectionFactory = connectionFactory;
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
     * or use the default implementation {@link com.okta.oidc.storage.SimpleOktaStorage}
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
     * Create auth client.
     *
     * @return the a AuthClient
     */
    @SuppressWarnings("WeakerAccess")
    protected A createAuthClient() {
        return mClientFactory.createClient();
    }

    /**
     * Gets okta state.
     *
     * @return the okta state
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public OktaState getOktaState() {
        return new OktaState(new OktaRepository(mStorage, mContext, mEncryptionManager));
    }
}
