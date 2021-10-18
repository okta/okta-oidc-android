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
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.clients.SyncAuthClient;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.sessions.SyncSessionClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static com.okta.oidc.net.request.ProviderConfiguration.OAUTH2_CONFIGURATION_RESOURCE;
import static com.okta.oidc.net.request.ProviderConfiguration.OPENID_CONFIGURATION_RESOURCE;

/**
 * Okta config information. This is used to setup a configuration for AuthClient and
 * SessionClient clients.
 * {@link AuthClient}
 * {@link SyncAuthClient}
 * {@link SessionClient}
 * {@link SyncSessionClient}
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * OIDCConfig config = new OIDCConfig.Builder()
 *     .clientId("{clientId}")
 *     .redirectUri("{redirectUri}")
 *     .endSessionRedirectUri("{endSessionUri}")
 *     .scopes("openid", "profile", "offline_access")
 *     .discoveryUri("https://{yourOktaDomain}")
 *     .create();
 * }
 * </pre>
 */
public class OIDCConfig {
    private static final String TAG = OIDCConfig.class.getSimpleName();
    private AccountInfo mAccount;
    private boolean mIsOAuth2Configuration;
    private CustomConfiguration mCustomConfiguration;
    private final OktaIdToken.Validator idTokenValidator;

    private OIDCConfig(AccountInfo account, OktaIdToken.Validator idTokenValidator) {
        mAccount = account;
        this.idTokenValidator = idTokenValidator;
        if (mAccount.mDiscoveryUri != null) {
            mIsOAuth2Configuration = mAccount.mDiscoveryUri.contains(OAUTH2_CONFIGURATION_RESOURCE)
                    && !mAccount.mDiscoveryUri.contains(OPENID_CONFIGURATION_RESOURCE);
        }
    }

    /**
     * Gets your Okta application client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return mAccount.mClientId;
    }

    /**
     * Returns the redirect uri to go to once the authorization log in flow is complete.
     * This must match the schema of the app's registered Uri provided in manifest
     *
     * @return the redirect uri
     */
    public Uri getRedirectUri() {
        return Uri.parse(mAccount.mRedirectUri);
    }

    /**
     * Returns the end session uri to go to once the authorization log out flow is complete.
     * This must match schema of the app's registered Uri provided in manifest
     *
     * @return the end session redirect uri
     */
    public Uri getEndSessionRedirectUri() {
        return Uri.parse(mAccount.mEndSessionRedirectUri);
    }

    /**
     * Returns the discovery uri for the authorization server. It is formed by appending the
     * well known location of the discovery document to the issuer. Can be null if the authorization
     * server does not provide a discovery end point.
     *
     * @return The Uri where the discovery document can be found or null if not provided.
     */
    @Nullable
    public Uri getDiscoveryUri() {
        if (mAccount.mDiscoveryUri == null) {
            return null;
        }
        if (mAccount.mDiscoveryUri.contains(OPENID_CONFIGURATION_RESOURCE)
                || mAccount.mDiscoveryUri.contains(OAUTH2_CONFIGURATION_RESOURCE)) {
            return Uri.parse(mAccount.mDiscoveryUri);
        } else {
            return Uri.parse(mAccount.mDiscoveryUri + (mIsOAuth2Configuration ?
                    OAUTH2_CONFIGURATION_RESOURCE : OPENID_CONFIGURATION_RESOURCE));
        }
    }

    /**
     * Check to see if the configuration is from OAuth2 or OpenID Connect.
     *
     * @return true if the authorization server is OAuth2 instead of OpenID Connect.
     */
    public boolean isOAuth2Configuration() {
        return mIsOAuth2Configuration;
    }

    /**
     * Returns the set of scopes defined by the configuration. These scopes can be used during
     * the authorization request for the user.
     *
     * @return The set of scopes defined by the configuration
     */
    public String[] getScopes() {
        return mAccount.mScopes;
    }

    /**
     * Returns the custom configuration if set.
     *
     * @return custom configuration
     */
    @Nullable
    public CustomConfiguration getCustomConfiguration() {
        return mCustomConfiguration;
    }

    /**
     * Returns the ID Token Validator.
     *
     * @return the ID Token Validator.
     */
    public OktaIdToken.Validator getIdTokenValidator() {
        return idTokenValidator;
    }

    private static class AccountInfo {
        @SerializedName("client_id")
        String mClientId;
        @SerializedName("redirect_uri")
        String mRedirectUri;
        @SerializedName("end_session_redirect_uri")
        String mEndSessionRedirectUri;
        @SerializedName("scopes")
        String[] mScopes;
        @SerializedName("discovery_uri")
        String mDiscoveryUri;

        AccountInfo() {
        }

        void validate(boolean useCustomConfig) {
            if (TextUtils.isEmpty(mClientId)) {
                throw new IllegalStateException("No client id specified");
            }
            if (TextUtils.isEmpty(mRedirectUri)) {
                throw new IllegalStateException("No redirect uri specified");
            }
            if (TextUtils.isEmpty(mEndSessionRedirectUri)) {
                throw new IllegalStateException("No end session specified");
            }
            if (!useCustomConfig && TextUtils.isEmpty(mDiscoveryUri)) {
                //Check for other
                throw new IllegalStateException("No discovery uri specified");
            }
            if (mScopes == null) {
                throw new IllegalStateException("No scopes specified");
            }
            //check for empty scope
            for (String scope : mScopes) {
                if (TextUtils.isEmpty(scope)) {
                    throw new IllegalStateException("Individual scopes cannot be null or empty");
                }
            }
        }
    }

    /**
     * The OIDCConfig Builder.
     */
    public static class Builder {
        private AccountInfo mAccountInfo;
        private CustomConfiguration mCustomConfiguration;
        private OktaIdToken.Validator mIdTokenValidator =
                new OktaIdToken.DefaultValidator(System::currentTimeMillis);

        /**
         * Instantiates a new Builder.
         */
        public Builder() {
            mAccountInfo = new AccountInfo();
        }

        /**
         * Create OIDC config.
         *
         * @return the config
         */
        public OIDCConfig create() {
            mAccountInfo.validate(mCustomConfiguration != null);
            OIDCConfig config = new OIDCConfig(mAccountInfo, mIdTokenValidator);
            config.mCustomConfiguration = mCustomConfiguration;
            return config;
        }

        /**
         * Client id of your Okta application.
         *
         * @param clientId Okta application client id
         * @return current builder
         */
        public Builder clientId(@NonNull String clientId) {
            mAccountInfo.mClientId = clientId;
            return this;
        }

        /**
         * Sets redirect uri to go to once the authorization log in flow is complete.
         * This must match the schema of the app's registered Uri provided in manifest
         *
         * @param redirect the redirect uri
         * @return current builder
         */
        public Builder redirectUri(@NonNull String redirect) {
            mAccountInfo.mRedirectUri = redirect;
            return this;
        }

        /**
         * Sets redirect uri to go to once the authorization log out flow is complete.
         * This must match the schema of the app's registered Uri provided in manifest
         *
         * @param endSessionRedirect the end session redirect
         * @return current builder
         */
        public Builder endSessionRedirectUri(@NonNull String endSessionRedirect) {
            mAccountInfo.mEndSessionRedirectUri = endSessionRedirect;
            return this;
        }

        /**
         * The discovery uri for the authorization server. This is your applications domain url
         *
         * @param discoveryUri the discovery uri
         * @return current builder
         */
        public Builder discoveryUri(@NonNull String discoveryUri) {
            mAccountInfo.mDiscoveryUri = discoveryUri;
            return this;
        }

        /**
         * Sets the scopes of the for authorization.
         *
         * @param scopes the scopes
         * @return current builder
         * @see <a href="https://developer.okta.com/docs/api/resources/oidc/#scopes">
         * Okta OIDC scopes</a>
         */
        public Builder scopes(@NonNull String... scopes) {
            mAccountInfo.mScopes = scopes;
            return this;
        }

        /**
         * Optional custom configuration. This can be used if authorization provider does not
         * provide a discovery endpoint. If discoveryUri is set this value will be ignored,
         * Open ID provider configuration will be fetched using the discoveryUri.
         *
         * @param customConfiguration the custom configuration if no discoveryUri is provided.
         * @return current builder
         */
        public Builder customConfiguration(@NonNull CustomConfiguration customConfiguration) {
            mCustomConfiguration = customConfiguration;
            return this;
        }

        /**
         * Optional custom ID Token validator. This can be used to fine tune the checks that are
         * done before saving the Tokens.
         *
         * <p>If not specified, it'll check the claims expiration time is at a future time, as well
         * as check the issued at time is within a 10 minute window of the current time.
         *
         * @param idTokenValidator the ID Token Validator
         * @return current builder
         */
        public Builder idTokenValidator(@NonNull OktaIdToken.Validator idTokenValidator) {
            mIdTokenValidator = idTokenValidator;
            return this;
        }

        /**
         * Sets the resource id of the configuration file in JSON format.
         *
         * @param context  a valid context
         * @param rawResId the android resource id
         * @return current builder
         */
        public Builder withJsonFile(Context context, @RawRes int rawResId) {
            try (InputStream inputStream = context.getResources().openRawResource(rawResId)) {
                Writer writer = new StringWriter();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
                JSONObject json = new JSONObject(writer.toString());
                readConfiguration(json);
            } catch (IOException | JSONException | JsonSyntaxException e) {
                Log.e(TAG, "Invalid JSON file", e);
                return null;
            }
            return this;
        }

        private void readConfiguration(@NonNull final JSONObject jsonObject)
                throws JsonSyntaxException {
            Gson gson = new Gson();
            mAccountInfo = gson.fromJson(jsonObject.toString(), AccountInfo.class);
        }
    }
}
