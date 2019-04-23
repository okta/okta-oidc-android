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

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.okta.oidc.net.params.GrantTypes;
import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.TokenRequest;
import com.okta.oidc.util.AuthorizationException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import static com.google.gson.stream.JsonToken.BEGIN_ARRAY;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR;

/**
 * The ID Token is a JSON Web Token (JWT) that contains information about an authentication event
 * and claims about the authenticated user. This information tells your client application that the
 * user is authenticated, and can also give you information like their username or locale.
 *
 * @see "ID Token
 * <https://developer.okta.com/docs/api/resources/oidc/#id-token>"
 * @see "Validating ID Tokens
 * <https://developer.okta.com/authentication-guide/tokens/validating-id-tokens/>"
 */
public class OktaIdToken {
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface Clock {
        long getCurrentTimeMillis();
    }

    @VisibleForTesting
    Header mHeader;
    @VisibleForTesting
    Claims mClaims;
    @VisibleForTesting
    String mSignature;

    private static final Long MILLIS_PER_SECOND = 1000L;
    private static final Long TEN_MINUTES_IN_SECONDS = 600L;

    public static class Address {
        public String street_address;
        public String locality;
        public String region;
        public String postal_code;
        public String country;
    }

    /**
     * Claims in the Header Section
     *
     * @see "ID Token Header
     * <https://developer.okta.com/docs/api/resources/oidc/#id-token-header>"
     */
    public static class Header {
        public String alg;
        public String kid;
    }

    /**
     * Claims in the payload are either base claims, independent of scope (always returned),
     * or dependent on scope (not always returned).
     *
     * @see "Claims in the Payload Section
     * <https://developer.okta.com/docs/api/resources/oidc/#id-token-header>"
     */
    public static class Claims {
        public List<String> amr;
        public List<String> aud;
        public int auth_time;
        public int exp;
        public int iat;
        public String idp;
        public String iss;
        public String jti;
        public String sub;
        public String ver;
        public String nonce;

        public String at_hash;

        public String name;
        public String preferred_username;
        public String nickname;
        public String given_name;
        public String middle_name;
        public String family_name;
        public String profile;
        public String zoneinfo;
        public String locale;
        public int updated_at;
        public String email;
        public String email_verified;
        public Address address;
        public String phone_number;
        public List<String> groups;
    }

    private OktaIdToken(Header header, Claims claims, String signature) {
        mHeader = header;
        mClaims = claims;
        mSignature = signature;
    }

    /**
     * Get the header claims {@link Header}
     */
    public Header getHeader() {
        return mHeader;
    }

    /**
     * Get the payload claims {@link Claims}
     */
    public Claims getClaims() {
        return mClaims;
    }

    /**
     * Get the signature
     *
     * @see "ID Token Signature
     * <https://developer.okta.com/docs/api/resources/oidc/#id-token-signature>"
     */
    public String getSignature() {
        return mSignature;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void validate(TokenRequest request, Clock clock) throws AuthorizationException {
        OIDCAccount account = request.getAccount();
        ProviderConfiguration config = request.getProviderConfiguration();

        if (!"RS256".equals(mHeader.alg)) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("JWT Header 'alg' of [" + mHeader.alg + "] " +
                            "is not supported, only RSA256 signatures are supported"));
        }

        if (!mClaims.iss.equals(config.issuer)) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("Issuer mismatch"));
        }

        Uri issuerUri = Uri.parse(mClaims.iss);
        if (!issuerUri.getScheme().equals("https")) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("Issuer must be an https URL"));
        }

        if (TextUtils.isEmpty(issuerUri.getHost())) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("Issuer host can not be empty"));
        }

        if (issuerUri.getFragment() != null || issuerUri.getQueryParameterNames().size() > 0) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException(
                            "Issuer URL contains query parameters or fragment components"));
        }

        String clientId = account.getClientId();
        if (!this.mClaims.aud.contains(clientId)) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("Audience mismatch"));
        }

        long nowInSeconds = clock.getCurrentTimeMillis() / MILLIS_PER_SECOND;
        if (nowInSeconds > mClaims.exp) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("ID Token expired"));
        }

        if (Math.abs(nowInSeconds - mClaims.iat) > TEN_MINUTES_IN_SECONDS) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("Issued at time is more than 10 minutes "
                            + "before or after the current time"));
        }

        if (GrantTypes.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            String expectedNonce = request.getNonce();
            if (!TextUtils.equals(mClaims.nonce, expectedNonce)) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        new IllegalStateException("Nonce mismatch"));
            }
        }

        if (request.getMaxAge() != null && mClaims.auth_time <= 0) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    new IllegalStateException("max_age provided but auth_time is missing"));
        }
    }

    /**
     * Parses a JSON Web Token (JWT).
     *
     * @param token the based64 encoded idToken
     */
    public static OktaIdToken parseIdToken(@NonNull String token) throws IllegalArgumentException {
        String[] sections = token.split("\\.");
        if (sections.length < 3) {
            throw new IllegalArgumentException("IdToken missing header, claims or" +
                    " signature section");
        }
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(ArrayTypeAdapter.CREATE).create();
        //decode header
        String headerSection = new String(Base64.decode(sections[0], Base64.URL_SAFE));
        Header header = gson.fromJson(headerSection, Header.class);
        //decode claims
        String claimsSection = new String(Base64.decode(sections[1], Base64.URL_SAFE));
        Claims claims = gson.fromJson(claimsSection, Claims.class);
        String signature = new String(Base64.decode(sections[2], Base64.URL_SAFE));
        return new OktaIdToken(header, claims, signature);
    }

    /*
     * Adapter needed for parsing audience which can be a single element or a array.
     * If audience is a single element then this adapter converts the single element audience
     * to a list of one element.
     */
    private static final class ArrayTypeAdapter extends TypeAdapter<List<Object>> {
        final TypeAdapter<List<Object>> mDelegate;
        final TypeAdapter<Object> mElement;

        static final TypeAdapterFactory CREATE = new TypeAdapterFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                if (type.getRawType() != List.class) {
                    return null;
                }
                Type elementType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
                TypeAdapter<List<Object>> delegateAdapter =
                        (TypeAdapter<List<Object>>) gson.getDelegateAdapter(this, type);
                TypeAdapter<Object> elementAdapter =
                        (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(elementType));
                return (TypeAdapter<T>) new ArrayTypeAdapter(delegateAdapter, elementAdapter);
            }
        };

        ArrayTypeAdapter(TypeAdapter<List<Object>> delegateAdapter,
                         TypeAdapter<Object> elementAdapter) {
            mDelegate = delegateAdapter;
            mElement = elementAdapter;
        }

        @Override
        public List<Object> read(JsonReader reader) throws IOException {
            if (reader.peek() != BEGIN_ARRAY) {
                return Collections.singletonList(mElement.read(reader));
            }
            return mDelegate.read(reader);
        }

        @Override
        public void write(JsonWriter writer, List<Object> value)
                throws IOException {
            if (value.size() == 1) {
                mElement.write(writer, value.get(0));
            } else {
                mDelegate.write(writer, value);
            }
        }
    }
}