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
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.okta.oidc.net.request.ProviderConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/*
    Okta OIDC application information
 */
public class OIDCAccount {
    private static final String TAG = OIDCAccount.class.getSimpleName();

    private AccountInfo mAccount;

    private OIDCAccount(AccountInfo account) {
        mAccount = account;
    }

    public String getClientId() {
        return mAccount.mClientId;
    }

    public Uri getRedirectUri() {
        return Uri.parse(mAccount.mRedirectUri);
    }

    public Uri getEndSessionRedirectUri() {
        return Uri.parse(mAccount.mEndSessionRedirectUri);
    }

    public Uri getDiscoveryUri() {
        return Uri.parse(mAccount.mDiscoveryUri);
    }

    public String[] getScopes() {
        return mAccount.mScopes;
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
        @SerializedName("issuer_uri")
        String mDiscoveryUri;

        AccountInfo() {
        }

        void validate() {
            if (mClientId == null || mClientId.isEmpty()) {
                throw new IllegalStateException("No client id specified");
            }
            if (mRedirectUri == null || mRedirectUri.isEmpty()) {
                throw new IllegalStateException("No redirect uri specified");
            }
            if (mEndSessionRedirectUri == null || mEndSessionRedirectUri.isEmpty()) {
                throw new IllegalStateException("No end session specified");
            }
            if (mDiscoveryUri == null || mDiscoveryUri.isEmpty()) {
                throw new IllegalStateException("No issuer uri specified");
            }
        }
    }

    public static class Builder {
        private AccountInfo mAccountInfo;

        public Builder() {
            mAccountInfo = new AccountInfo();
        }

        public OIDCAccount create() {
            mAccountInfo.mDiscoveryUri += ProviderConfiguration.OPENID_CONFIGURATION_RESOURCE;
            mAccountInfo.validate();
            return new OIDCAccount(mAccountInfo);
        }

        public Builder clientId(@NonNull String clientId) {
            mAccountInfo.mClientId = clientId;
            return this;
        }

        public Builder redirectUri(@NonNull String redirect) {
            mAccountInfo.mRedirectUri = redirect;
            return this;
        }

        public Builder endSessionRedirectUri(@NonNull String endSessionRedirect) {
            mAccountInfo.mEndSessionRedirectUri = endSessionRedirect;
            return this;
        }

        public Builder discoveryUri(@NonNull String discoveryUri) {
            mAccountInfo.mDiscoveryUri = discoveryUri;
            return this;
        }

        public Builder scopes(@NonNull String... scopes) {
            mAccountInfo.mScopes = scopes;
            return this;
        }

        public Builder withResId(Context context, @RawRes int Id) {
            try (InputStream inputStream = context.getResources().openRawResource(Id)) {
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
            } catch (IOException e) {
                Log.e(TAG, "", e);
                return null;
            } catch (JSONException e) {
                Log.e(TAG, "", e);
                return null;
            } catch (JsonSyntaxException e) {
                Log.e(TAG, "", e);
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