package com.okta.oidc.clients.sessions;

import com.okta.oidc.storage.security.EncryptionManager;
import com.okta.oidc.util.AuthorizationException;

interface BaseSessionClient {
    /**
     * Checks to see if the user is authenticated. If the client have a access or ID token then
     * the user is considered authenticated and this call will return true. This does not check the
     * validity of the access token which could be expired or revoked.
     *
     * @return the boolean
     */
    boolean isAuthenticated();

    /**
     * Use this method to migrate to another Encryption Manager. This method should decrypt data
     * using current EncryptionManager and encrypt with new one. All follow data will be encrypted
     * by new Encryption Manager
     * @param manager   new Encryption Manager
     * @throws AuthorizationException
     */
    void migrateTo(EncryptionManager manager) throws AuthorizationException;
}
