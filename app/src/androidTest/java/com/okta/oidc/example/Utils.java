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
package com.okta.oidc.example;

import android.content.Context;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import static com.okta.oidc.example.BuildConfig.PASSWORD_MFA;
import static com.okta.oidc.example.BuildConfig.USERNAME_MFA;

final class Utils {
    private static final int BUFFER_SIZE = 1024;
    //apk package names
    @SuppressWarnings("unused")
    public static final String FIRE_FOX = "org.mozilla.firefox";
    public static final String CHROME_STABLE = "com.android.chrome";
    public static final String SAMPLE_APP = "com.okta.oidc.example";

    //timeout for app transition from browser to app.
    public static final int TRANSITION_TIMEOUT = 2000;
    public static final int NETWORK_TIMEOUT = 5000;
    public static final int NOTIFICATION_TIMEOUT = 5000;
    public static final int WAIT_FOR_POLL_TIMEOUT = 5000;
    public static final int RECENTS_TIMEOUT = 1500;
    public static final int EXCHANGE_TIMEOUT = 7000;

    //web page resource ids
    public static final String ID_USERNAME = "okta-signin-username";
    public static final String ID_PASSWORD = "okta-signin-password";
    public static final String ID_SUBMIT = "okta-signin-submit";
    public static final String ID_NO_THANKS = "com.android.chrome:id/negative_button";
    public static final String ID_ACCEPT = "com.android.chrome:id/terms_accept";
    public static final String ID_CLOSE_BROWSER = "com.android.chrome:id/close_button";

    public static final String TEXT_SEND_PUSH = "Send Push";
    public static final String TEXT_APPROVE = "Approve";
    public static final String TEXT_DENY = "Deny";

    public static String PASSWORD = BuildConfig.PASSWORD;
    public static String USERNAME = BuildConfig.USERNAME;

    public static void acceptChromePrivacyOption(UiDevice device) throws UiObjectNotFoundException {
        UiSelector selector = new UiSelector();
        UiObject accept = device.findObject(selector.resourceId(ID_ACCEPT));
        accept.waitForExists(TRANSITION_TIMEOUT);
        if (accept.exists()) {
            accept.click();
        }

        UiObject noThanks = device.findObject(selector.resourceId(ID_NO_THANKS));
        noThanks.waitForExists(TRANSITION_TIMEOUT);
        if (noThanks.exists()) {
            noThanks.click();
        }
    }

    public static void customTabInteraction(UiDevice device, boolean enterUserName)
            throws UiObjectNotFoundException {
        device.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        customTabInteraction(device, enterUserName, USERNAME, PASSWORD);
        device.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
    }

    public static void customTabInteractionWithMfa(UiDevice device, boolean enterUserName)
            throws UiObjectNotFoundException {
        device.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        UiObject sendPush = device.findObject(new UiSelector().text(TEXT_SEND_PUSH));
        sendPush.waitForExists(TRANSITION_TIMEOUT);
        if (!sendPush.exists()) { //user already logged in.
            customTabInteraction(device, enterUserName, USERNAME_MFA, PASSWORD_MFA);
        }
    }

    private static void customTabInteraction(UiDevice device, boolean enterUserName, String name,
                                             String password) throws UiObjectNotFoundException {
        acceptChromePrivacyOption(device);
        UiSelector selector = new UiSelector();
        if (enterUserName) {
            UiObject username = device.findObject(selector.resourceId(ID_USERNAME));
            username.setText(name);
        }
        UiObject uiPassword = device.findObject(selector.resourceId(ID_PASSWORD));
        uiPassword.setText(password);
        UiObject signIn = device.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();
    }

    static String getAsset(Context context, String filename) {
        try {
            StringBuilder builder = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(
                    context.getResources().getAssets().open(filename), "UTF-8");

            char[] buffer = new char[BUFFER_SIZE];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJwt(String issuer, String nonce, Date expiredDate, Date issuedAt,
                                String... audience) {
        JwtBuilder builder = Jwts.builder();
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        Map<String, Object> map = new HashMap<>();
        map.put(Claims.AUDIENCE, Arrays.asList(audience));

        return builder
                .addClaims(map)
                .claim("nonce", nonce)
                .setIssuer(issuer)
                .setSubject("sub")
                .setExpiration(expiredDate)
                .setIssuedAt(issuedAt)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    public static Date getNow() {
        long nowMillis = System.currentTimeMillis();
        return new Date(nowMillis);
    }

    public static Date getTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }

    public static Date getYesterday() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, -1);
        return c.getTime();
    }

    public static Date getExpiredFromTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTime(getNow());
        c.add(Calendar.DATE, 2);
        return c.getTime();
    }
}
