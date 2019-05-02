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

package com.okta.oidc.storage.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * TODO .
 */
public class SimpleEncryptionManager implements EncryptionManager {
    private static final int RSA_BIT_LENGTH = 2048;
    private static final int AES_BIT_LENGTH = 256;
    private static final int MAC_BIT_LENGTH = 256;
    private static final int GCM_TAG_LENGTH = 128;

    private static final int COMPAT_IV_LENGTH = 16;
    private static final int IV_LENGTH = 12;

    private static final String DEFAULT_CHARSET = "UTF-8";

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String SSL_PROVIDER = "AndroidOpenSSL";
    private static final String BOUNCY_CASTLE_PROVIDER = "BC";

    private static final String RSA_KEY_ALIAS_NAME = "rsa_key";
    private static final String AES_KEY_ALIAS_NAME = "aes_key";
    private static final String MAC_KEY_ALIAS_NAME = "mac_key";

    private static final String OVERRIDING_KEY_ALIAS_PREFIX_NAME = "OverridingAlias";
    private static final String DEFAULT_KEY_ALIAS_PREFIX = "sps";

    private static final String DELIMITER = "]";

    private static final String KEY_ALGORITHM_AES = "AES";
    private static final String KEY_ALGORITHM_RSA = "RSA";

    private static final String BLOCK_MODE_ECB = "ECB";
    private static final String BLOCK_MODE_GCM = "GCM";
    private static final String BLOCK_MODE_CBC = "CBC";

    private static final String ENCRYPTION_PADDING_RSA_PKCS1 = "PKCS1Padding";
    private static final String ENCRYPTION_PADDING_PKCS7 = "PKCS7Padding";
    private static final String ENCRYPTION_PADDING_NONE = "NoPadding";
    private static final String MAC_ALGORITHM_HMAC_SHA256 = "HmacSHA256";

    private static final String RSA_CIPHER = KEY_ALGORITHM_RSA + "/" +
            BLOCK_MODE_ECB + "/" +
            ENCRYPTION_PADDING_RSA_PKCS1;
    private static final String AES_CIPHER = KEY_ALGORITHM_AES + "/" +
            BLOCK_MODE_GCM + "/" +
            ENCRYPTION_PADDING_NONE;
    private static final String AES_CIPHER_COMPAT = KEY_ALGORITHM_AES + "/" +
            BLOCK_MODE_CBC + "/" +
            ENCRYPTION_PADDING_PKCS7;
    private static final String MAC_CIPHER = MAC_ALGORITHM_HMAC_SHA256;

    private static final String IS_COMPAT_MODE_KEY_ALIAS_NAME = "data_in_compat";

    private static final int RSA_CALENDAR_HOURS_OFFSET = -26;
    private static final int RSA_CALENDAR_MAX_YEARS = 100;

    private final byte[] shiftingKey;

    private final String rsaKeyAlias;
    private final String aesKeyAlias;
    private final String macKeyAlias;

    private final String isCompatModeKeyAlias;

    private KeyStore mStore;
    private SecretKey aesKey;
    private SecretKey macKey;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    private String mKeyAliasPrefix;


    private boolean isCompatMode;

    /**
     * builds configured instance of SimpleEncryptionManager.
     *
     * @param context application context
     * @throws IOException if has issues with IO
     * @throws CertificateException if certificates can not be generated.
     * @throws NoSuchAlgorithmException if the algorithm for recovering the
     *                                  entry cannot be found
     * @throws KeyStoreException if no keystore present
     * @throws UnrecoverableEntryException if the specified
     *                                     {@link java.security.KeyStore.ProtectionParameter}
     *                                     were insufficient or invalid
     * @throws InvalidAlgorithmParameterException if algorithms are parameterized wrong
     * @throws NoSuchPaddingException if padding setup improperly
     * @throws InvalidKeyException if RSA keys are not working
     * @throws NoSuchProviderException if keys provider
     */
    public SimpleEncryptionManager(Context context)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
            UnrecoverableEntryException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            InvalidKeyException, NoSuchProviderException {

        this(context, null, null);
    }

    private SimpleEncryptionManager(Context context, @Nullable String keyAliasPrefix,
                            @Nullable byte[] bitShiftingKey)
            throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            NoSuchProviderException, NoSuchPaddingException, CertificateException,
            KeyStoreException, UnrecoverableEntryException, InvalidKeyException,
            IllegalStateException {

        shiftingKey = bitShiftingKey;
        SharedPreferences prefStore = context.getSharedPreferences(
                SimpleEncryptionManager.class.getCanonicalName(), Context.MODE_PRIVATE);
        keyAliasPrefix = prefStore.getString(getHashed(OVERRIDING_KEY_ALIAS_PREFIX_NAME),
                keyAliasPrefix);
        mKeyAliasPrefix = keyAliasPrefix != null ? keyAliasPrefix : DEFAULT_KEY_ALIAS_PREFIX;
        isCompatModeKeyAlias = String.format("%s_%s", mKeyAliasPrefix,
                IS_COMPAT_MODE_KEY_ALIAS_NAME);
        rsaKeyAlias = String.format("%s_%s", mKeyAliasPrefix, RSA_KEY_ALIAS_NAME);
        aesKeyAlias = String.format("%s_%s", mKeyAliasPrefix, AES_KEY_ALIAS_NAME);
        macKeyAlias = String.format("%s_%s", mKeyAliasPrefix, MAC_KEY_ALIAS_NAME);

        String isCompatKey = getHashed(isCompatModeKeyAlias);
        isCompatMode = prefStore.getBoolean(isCompatKey,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M);

        loadKeyStore();

        try {
            setup(context, prefStore, bitShiftingKey);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private void setup(Context context, SharedPreferences prefStore, @Nullable byte[] seed)
            throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            KeyStoreException, UnrecoverableEntryException, NoSuchProviderException,
            InvalidAlgorithmParameterException, IOException {
        boolean keyGenerated = generateKey(context, seed, prefStore);
        if (keyGenerated) {
            //store the alias prefix
            prefStore.edit().putString(getHashed(OVERRIDING_KEY_ALIAS_PREFIX_NAME),
                    mKeyAliasPrefix).apply();
        }

        loadKey(prefStore);
    }

    private EncryptedData tryEncrypt(byte[] bytes) throws NoSuchPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException,
            BadPaddingException, IllegalBlockSizeException, NoSuchProviderException,
            InvalidKeyException {
        EncryptedData result;
        try {
            result = encrypt(bytes);
        } catch (Exception ex) {
            throw ex;
        }
        return result;
    }

    private byte[] tryDecrypt(EncryptedData data) throws NoSuchPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, IOException, BadPaddingException,
            IllegalBlockSizeException, InvalidMacException {
        byte[] result;

        try {
            result = decrypt(data);
        } catch (Exception ex) {
            throw ex;
        }

        return result;
    }

    private EncryptedData encrypt(byte[] bytes) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, NoSuchProviderException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException {
        if (bytes != null && bytes.length > 0) {
            byte[] iv = getIv();
            if (isCompatMode) {
                return encryptAesCompat(bytes, iv);
            }
            else {
                return encryptAes(bytes, iv);
            }
        }

        return null;
    }

    @Override
    public String encrypt(String text) throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IOException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException, NoSuchProviderException, BadPaddingException {
        if (text != null && text.length() > 0) {
            EncryptedData encrypted = tryEncrypt(text.getBytes(DEFAULT_CHARSET));
            return encodeEncryptedData(encrypted);
        }

        return null;
    }

    private byte[] decrypt(EncryptedData data) throws NoSuchPaddingException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidMacException, NoSuchProviderException, InvalidKeyException {
        if (data != null && data.encryptedData != null) {
            if (isCompatMode) {
                return decryptAesCompat(data);
            }
            else {
                return decryptAes(data);
            }
        }
        return null;
    }

    @Override
    public String decrypt(String text) throws IOException, NoSuchPaddingException,
            InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidMacException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        if (text != null && text.length() > 0) {
            EncryptedData encryptedData = decodeEncryptedText(text);
            byte[] decrypted = tryDecrypt(encryptedData);

            return new String(decrypted, 0, decrypted.length, DEFAULT_CHARSET);
        }

        return null;
    }

    @Override
    public String getHashed(String text) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] result = digest.digest(text.getBytes(DEFAULT_CHARSET));

        return toHex(result);
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    private static String base64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private static byte[] base64Decode(String text) {
        return Base64.decode(text, Base64.NO_WRAP);
    }

    String encodeEncryptedData(EncryptedData data) {
        if (data.mac != null) {
            return base64Encode(data.iv) + DELIMITER + base64Encode(data.encryptedData)
                    + DELIMITER + base64Encode(data.mac);
        } else {
            return base64Encode(data.iv) + DELIMITER + base64Encode(data.encryptedData);
        }
    }

    EncryptedData decodeEncryptedText(String text) {
        EncryptedData result = new EncryptedData();
        String[] parts = text.split(DELIMITER);
        result.iv = base64Decode(parts[0]);
        result.encryptedData = base64Decode(parts[1]);

        if (parts.length > 2) {
            result.mac = base64Decode(parts[2]);
        }

        return result;
    }

    private void loadKeyStore() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        mStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        mStore.load(null);
    }

    private byte[] getIv() {
        byte[] iv;
        if (!isCompatMode) {
            iv = new byte[IV_LENGTH];
        } else {
            iv = new byte[COMPAT_IV_LENGTH];
        }
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(iv);
        return iv;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Cipher getCipherAes(byte[] iv, boolean modeEncrypt) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(modeEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, aesKey,
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return cipher;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private EncryptedData encryptAes(byte[] bytes, byte[] iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = getCipherAes(iv, true);
        EncryptedData result = new EncryptedData();
        result.iv = cipher.getIV();
        result.encryptedData = cipher.doFinal(bytes);

        return result;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private byte[] decryptAes(EncryptedData encryptedData) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = getCipherAes(encryptedData.iv, false);
        return cipher.doFinal(encryptedData.encryptedData);
    }

    private Cipher getCipherAesCompat(byte[] iv, boolean modeEncrypt) throws NoSuchPaddingException,
            NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER_COMPAT);
        cipher.init(modeEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, aesKey,
                new IvParameterSpec(iv));

        return cipher;
    }

    private  EncryptedData encryptAesCompat(byte[] bytes, byte[] iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = getCipherAesCompat(iv, true);
        EncryptedData result = new EncryptedData();
        result.iv = cipher.getIV();
        result.encryptedData = cipher.doFinal(bytes);
        result.mac = computeMac(result.getDataForMacComputation());

        return result;
    }

    private byte[] decryptAesCompat(EncryptedData encryptedData) throws NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException,
            InvalidMacException {
        if (verifyMac(encryptedData.mac, encryptedData.getDataForMacComputation())) {
            Cipher cipher = getCipherAesCompat(encryptedData.iv, false);
            return cipher.doFinal(encryptedData.encryptedData);
        } else {
            throw new InvalidMacException();
        }
    }

    private void loadKey(SharedPreferences prefStore) throws KeyStoreException,
            UnrecoverableEntryException, NoSuchAlgorithmException, NoSuchPaddingException,
            NoSuchProviderException, InvalidKeyException, IOException {
        if (!isCompatMode) {
            if (mStore.containsAlias(aesKeyAlias) && mStore.entryInstanceOf(aesKeyAlias,
                    KeyStore.SecretKeyEntry.class)) {
                KeyStore.SecretKeyEntry entry =
                        (KeyStore.SecretKeyEntry) mStore.getEntry(aesKeyAlias, null);
                aesKey = entry.getSecretKey();
            }
        } else {
            aesKey = getFallbackAesKey(prefStore);
            macKey = getMacKey(prefStore);
        }
    }

    private boolean generateKey(Context context, @Nullable byte[] seed, SharedPreferences prefStore)
            throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException, NoSuchPaddingException,
            InvalidKeyException, IOException {
        boolean keyGenerated;

        if (!isCompatMode) {
            keyGenerated = generateAesKey(seed);
        } else {
            keyGenerated = generateRsaKeys(context, seed);
            loadRsaKeys();
            keyGenerated = generateFallbackAesKey(prefStore, seed) || keyGenerated;
            keyGenerated = generateMacKey(prefStore, seed) || keyGenerated;
        }

        return keyGenerated;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean generateAesKey(@Nullable byte[] seed) throws KeyStoreException,
            NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (!mStore.containsAlias(aesKeyAlias)) {
            KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER);

            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(aesKeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setCertificateSubject(
                            new X500Principal("CN = Secured Preference Store, O = Devliving Online")
                    )
                    .setCertificateSerialNumber(BigInteger.ONE)
                    .setKeySize(AES_BIT_LENGTH)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(false)
                    //TODO: set to true and let the Cipher generate a secured iv
                    .build();
            if (seed != null && seed.length > 0) {
                SecureRandom random = new SecureRandom(seed);
                keyGen.init(spec, random);
            } else {
                keyGen.init(spec);
            }

            keyGen.generateKey();

            return true;
        }

        return false;
    }

    private boolean generateFallbackAesKey(SharedPreferences prefStore, @Nullable byte[] seed)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, NoSuchProviderException {
        String key = getHashed(aesKeyAlias);

        if (!prefStore.contains(key)) {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM_AES);

            if (seed != null && seed.length > 0) {
                SecureRandom random = new SecureRandom(seed);
                keyGen.init(AES_BIT_LENGTH, random);
            } else {
                keyGen.init(AES_BIT_LENGTH);
            }

            SecretKey secretKey = keyGen.generateKey();

            byte[] shiftedEncodedKey = xorWithKey(secretKey.getEncoded(), shiftingKey);
            byte[] encryptedData = rsaEncrypt(shiftedEncodedKey);

            String aesFallbackKey = base64Encode(encryptedData);
            boolean result = prefStore.edit().putString(key, aesFallbackKey).commit();
            String isCompatKey = getHashed(isCompatModeKeyAlias);
            prefStore.edit().putBoolean(isCompatKey, true).apply();
            return result;
        }

        return false;
    }

    private boolean generateMacKey(SharedPreferences prefStore, @Nullable byte[] seed)
            throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        String key = getHashed(macKeyAlias);

        if (!prefStore.contains(key)) {
            byte[] randomBytes = new byte[MAC_BIT_LENGTH / Byte.SIZE];
            SecureRandom rng;
            if (seed != null && seed.length > 0) {
                rng = new SecureRandom(seed);
            } else {
                rng = new SecureRandom();
            }

            rng.nextBytes(randomBytes);

            byte[] encryptedKey = rsaEncrypt(randomBytes);
            String encodedKey = base64Encode(encryptedKey);
            return prefStore.edit().putString(key, encodedKey).commit();
        }

        return false;
    }

    private byte[] xorWithKey(byte[] a, byte[] key) {
        if (key == null || key.length == 0) {
            return a;
        }

        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ key[i % key.length]);
        }
        return out;
    }

    private SecretKey getFallbackAesKey(SharedPreferences prefStore) throws IOException,
            NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
            NoSuchPaddingException {
        String key = getHashed(aesKeyAlias);

        String base64Value = prefStore.getString(key, null);
        if (base64Value != null) {
            byte[] encryptedData = base64Decode(base64Value);
            byte[] shiftedEncodedKey = rsaDecrypt(encryptedData);
            byte[] keyData = xorWithKey(shiftedEncodedKey, shiftingKey);

            return new SecretKeySpec(keyData, "AES");
        }

        return null;
    }

    private SecretKey getMacKey(SharedPreferences prefStore) throws IOException,
            NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
            NoSuchPaddingException {
        String key = getHashed(macKeyAlias);

        String base64 = prefStore.getString(key, null);
        if (base64 != null) {
            byte[] encryptedKey = base64Decode(base64);
            byte[] keyData = rsaDecrypt(encryptedKey);

            return new SecretKeySpec(keyData, MAC_CIPHER);
        }

        return null;
    }

    private void loadRsaKeys() throws KeyStoreException, UnrecoverableEntryException,
            NoSuchAlgorithmException {
        if (mStore.containsAlias(rsaKeyAlias)
                && mStore.entryInstanceOf(rsaKeyAlias, KeyStore.PrivateKeyEntry.class)) {
            KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry) mStore.getEntry(rsaKeyAlias, null);
            publicKey = (RSAPublicKey) entry.getCertificate().getPublicKey();
            privateKey = (RSAPrivateKey) entry.getPrivateKey();
        }
    }

    @SuppressWarnings("WrongConstant")
    private boolean generateRsaKeys(Context context, @Nullable byte[] seed)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, KeyStoreException {
        if (!mStore.containsAlias(rsaKeyAlias)) {
            KeyPairGenerator keyGen =
                    KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);

            KeyPairGeneratorSpec spec;
            Calendar start = Calendar.getInstance();
            //probable fix for the timezone issue
            start.add(Calendar.HOUR_OF_DAY, RSA_CALENDAR_HOURS_OFFSET);

            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, RSA_CALENDAR_MAX_YEARS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(rsaKeyAlias)
                        .setKeySize(RSA_BIT_LENGTH)
                        .setKeyType(KEY_ALGORITHM_RSA)
                        .setSerialNumber(BigInteger.ONE)
                        .setSubject(new X500Principal(
                                "CN = Secured Preference Store, O = Devliving Online"))
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
            } else {
                spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(rsaKeyAlias)
                        .setSerialNumber(BigInteger.ONE)
                        .setSubject(new X500Principal(
                                "CN = Secured Preference Store, O = Devliving Online"))
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
            }

            if (seed != null && seed.length > 0) {
                SecureRandom random = new SecureRandom(seed);
                keyGen.initialize(spec, random);
            } else {
                keyGen.initialize(spec);
            }
            keyGen.generateKeyPair();

            return true;
        }

        return false;
    }

    private byte[] computeMac(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha256 = Mac.getInstance(MAC_CIPHER);
        hmacSha256.init(macKey);
        return hmacSha256.doFinal(data);
    }

    private boolean verifyMac(byte[] mac, byte[] data) throws InvalidKeyException,
            NoSuchAlgorithmException {
        if (mac != null && data != null) {
            byte[] actualMac = computeMac(data);

            if (actualMac.length != mac.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < actualMac.length; i++) {
                result |= actualMac[i] ^ mac[i];
            }
            return result == 0;
        }

        return false;
    }

    private byte[] rsaEncrypt(byte[] bytes) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IOException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER, SSL_PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
        cipherOutputStream.write(bytes);
        cipherOutputStream.close();

        return outputStream.toByteArray();
    }

    private byte[] rsaDecrypt(byte[] bytes) throws NoSuchPaddingException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, IOException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER, SSL_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(bytes),
                cipher);

        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] dbytes = new byte[values.size()];
        for (int i = 0; i < dbytes.length; i++) {
            dbytes[i] = values.get(i);
        }

        cipherInputStream.close();
        return dbytes;
    }

    static class EncryptedData {
        byte[] iv;
        byte[] encryptedData;
        byte[] mac;

        EncryptedData() {
            iv = null;
            encryptedData = null;
            mac = null;
        }

        /**
         * @return iv + CIPHER
         */
        byte[] getDataForMacComputation() {
            byte[] combinedData = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combinedData, 0, iv.length);
            System.arraycopy(encryptedData, 0, combinedData, iv.length, encryptedData.length);

            return combinedData;
        }
    }

    class InvalidMacException extends GeneralSecurityException {
        InvalidMacException() {
            super("Invalid Mac, failed to verify integrity.");
        }
    }
}
