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
 * @see "ID Token <https://developer.okta.com/docs/api/resources/oidc/#id-token>"
 * @see "Validating ID Tokens <https://developer.okta.com/authentication-guide/tokens/validating-id-tokens/>"
 */
@SuppressWarnings("unused")
public class OktaIdToken {
    private static final int NUMBER_OF_SECTIONS = 3;

    /**
     * The interface Clock.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface Clock {
        /**
         * Gets current time millis.
         *
         * @return the current time millis
         */
        long getCurrentTimeMillis();
    }

    /**
     * Used to validate time dependant checks in the ID Token before saving the Tokens.
     */
    public interface Validator {
        /**
         * Should validate the time dependant checks in the ID Token.
         *
         * @param oktaIdToken the OktaIdToken to validate.
         * @throws AuthorizationException the authorization exception
         */
        void validate(OktaIdToken oktaIdToken) throws AuthorizationException;
    }

    /**
     * The header of a idToken.
     * {@link Header}
     */
    @VisibleForTesting
    Header mHeader;

    /**
     * The claims section of a idToken.
     * {@link Claims}
     */
    @VisibleForTesting
    Claims mClaims;

    /**
     * The signature of a idToken.
     */
    @VisibleForTesting
    String mSignature;

    private static final Long MILLIS_PER_SECOND = 1000L;
    private static final int SECONDS_IN_ONE_MINUTE = 60;
    private static final Long TEN_MINUTES_IN_SECONDS = 10L * SECONDS_IN_ONE_MINUTE;

    /**
     * The address in the claims section.
     */
    public static class Address {
        /**
         * The Street address.
         */
        public String street_address;
        /**
         * The Locality.
         */
        public String locality;
        /**
         * The Region.
         */
        public String region;
        /**
         * The Postal code.
         */
        public String postal_code;
        /**
         * The Country.
         */
        public String country;
    }

    /**
     * Claims in the Header Section.
     *
     * @see "ID Token Header <https://developer.okta.com/docs/api/resources/oidc/#id-token-header>"
     */
    public static class Header {
        /**
         * The Alg.
         */
        public String alg;
        /**
         * The Kid.
         */
        public String kid;
    }

    /**
     * Claims in the payload are either base claims, independent of scope (always returned),
     * or dependent on scope (not always returned).
     *
     * @see "Claims in the Payload Section <https://developer.okta.com/docs/api/resources/oidc/#id-token-header>"
     */
    public static class Claims {
        /**
         * The Amr.
         */
        public List<String> amr;
        /**
         * The Aud.
         */
        public List<String> aud;
        /**
         * The Auth time.
         */
        public int auth_time;
        /**
         * The Exp.
         */
        public int exp;
        /**
         * The Iat.
         */
        public int iat;
        /**
         * The Idp.
         */
        public String idp;
        /**
         * The Iss.
         */
        public String iss;
        /**
         * The Jti.
         */
        public String jti;
        /**
         * The Sub.
         */
        public String sub;
        /**
         * The Ver.
         */
        public String ver;
        /**
         * The Nonce.
         */
        public String nonce;

        /**
         * The At hash.
         */
        public String at_hash;

        /**
         * The Name.
         */
        public String name;
        /**
         * The Preferred username.
         */
        public String preferred_username;
        /**
         * The Nickname.
         */
        public String nickname;
        /**
         * The Given name.
         */
        public String given_name;
        /**
         * The Middle name.
         */
        public String middle_name;
        /**
         * The Family name.
         */
        public String family_name;
        /**
         * The Profile.
         */
        public String profile;
        /**
         * The Zoneinfo.
         */
        public String zoneinfo;
        /**
         * The Locale.
         */
        public String locale;
        /**
         * The Updated at.
         */
        public int updated_at;
        /**
         * The Email.
         */
        public String email;
        /**
         * The Email verified.
         */
        public String email_verified;
        /**
         * The Address.
         */
        public Address address;
        /**
         * The Phone number.
         */
        public String phone_number;
        /**
         * The Groups.
         */
        public List<String> groups;
    }

    private OktaIdToken(Header header, Claims claims, String signature) {
        mHeader = header;
        mClaims = claims;
        mSignature = signature;
    }

    /**
     * Get the header claims. {@link Header}
     *
     * @return the header
     */
    public Header getHeader() {
        return mHeader;
    }

    /**
     * Get the payload claims. {@link Claims}
     *
     * @return the claims
     */
    public Claims getClaims() {
        return mClaims;
    }

    /**
     * Get the signature.
     *
     * @return the signature
     * @see "ID Token Signature <https://developer.okta.com/docs/api/resources/oidc/#id-token-signature>"
     */
    public String getSignature() {
        return mSignature;
    }

    /**
     * Validate.
     *
     * @param request the request
     * @throws AuthorizationException the authorization exception
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void validate(TokenRequest request, Validator validator) throws AuthorizationException {
        final OIDCConfig config = request.getConfig();
        ProviderConfiguration providerConfig = request.getProviderConfiguration();

        if (!"RS256".equals(mHeader.alg)) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    AuthorizationException.TokenValidationError
                            .createNotSupportedAlgorithmException(mHeader.alg));
        }
        if (providerConfig.issuer != null) {
            if (!mClaims.iss.equals(providerConfig.issuer)) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError.ISSUER_MISMATCH);
            }

            Uri issuerUri = Uri.parse(mClaims.iss);
            if (!issuerUri.getScheme().equals("https")) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError.ISSUER_NOT_HTTPS_URL);
            }

            if (TextUtils.isEmpty(issuerUri.getHost())) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError.ISSUER_HOST_EMPTY);
            }

            if (issuerUri.getFragment() != null || issuerUri.getQueryParameterNames().size() > 0) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError
                                .ISSUER_URL_CONTAIN_OTHER_COMPONENTS);
            }
        }

        String clientId = config.getClientId();
        if (!this.mClaims.aud.contains(clientId)) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    AuthorizationException.TokenValidationError.AUDIENCE_MISMATCH);
        }

        validator.validate(this);

        if (GrantTypes.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            String expectedNonce = request.getNonce();
            if (!TextUtils.equals(mClaims.nonce, expectedNonce)) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError.NONCE_MISMATCH);
            }
        }

        if (request.getMaxAge() != null && mClaims.auth_time <= 0) {
            throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                    AuthorizationException.TokenValidationError.AUTH_TIME_MISSING);
        }
    }

    /**
     * The default ID Token Validator.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final class DefaultValidator implements Validator {
        private final Clock clock;

        /**
         * The default ID Token validator.
         *
         * @param clock the clock to use for the time validation.
         */
        public DefaultValidator(Clock clock) {
            this.clock = clock;
        }

        /**
         * Checks the claims expiration time is at a future time, as well as check the issued at
         * time is within a 10 minute window of the current time.
         *
         * @param oktaIdToken the OktaIdToken to validate.
         * @throws AuthorizationException the authorization exception
         */
        @Override public void validate(OktaIdToken oktaIdToken) throws AuthorizationException {
            long nowInSeconds = clock.getCurrentTimeMillis() / MILLIS_PER_SECOND;
            if (nowInSeconds > oktaIdToken.mClaims.exp) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError.ID_TOKEN_EXPIRED);
            }

            if (Math.abs(nowInSeconds - oktaIdToken.mClaims.iat) > TEN_MINUTES_IN_SECONDS) {
                throw AuthorizationException.fromTemplate(ID_TOKEN_VALIDATION_ERROR,
                        AuthorizationException.TokenValidationError.createWrongTokenIssuedTime(
                                TEN_MINUTES_IN_SECONDS.intValue() / SECONDS_IN_ONE_MINUTE));
            }
        }
    }

    /**
     * Parses a JSON Web Token (JWT).
     *
     * @param token the based64 encoded idToken
     * @return the okta id token
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static OktaIdToken parseIdToken(@NonNull String token) throws IllegalArgumentException {
        String[] sections = token.split("\\.");
        if (sections.length < NUMBER_OF_SECTIONS) {
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
        /**
         * The M delegate.
         */
        final TypeAdapter<List<Object>> mDelegate;
        /**
         * The M element.
         */
        final TypeAdapter<Object> mElement;

        /**
         * The Create.
         */
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

        /**
         * Instantiates a new Array type adapter.
         *
         * @param delegateAdapter the delegate adapter
         * @param elementAdapter  the element adapter
         */
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
