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

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.okta.oidc.Okta;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.CodeVerifierUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.okta.oidc.example.Utils.getAsset;
import static com.okta.oidc.example.Utils.getNow;
import static com.okta.oidc.example.Utils.getTomorrow;
import static com.okta.oidc.example.WireMockStubs.mockConfigurationRequest;
import static com.okta.oidc.example.WireMockStubs.mockTokenRequest;
import static com.okta.oidc.example.WireMockStubs.mockWebAuthorizeRequest;
import static com.okta.oidc.net.HttpConnection.USER_AGENT_HEADER;
import static com.okta.oidc.net.HttpConnection.X_OKTA_USER_AGENT;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.core.StringContains.containsString;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WireMockTest {
    private static final int HTTPS_PORT = 8443;
    private static final int PORT = 8080;
    private static final String KEYSTORE_PASSWORD = "123456";
    private static final String KEYSTORE_PATH = "/sdcard/Download/mock.keystore.bks";
    //must match issuer in configuration.json
    private static final String ISSUER = "https://127.0.0.1:8443/mocktest";
    //apk package names
    private static final String FIRE_FOX = "org.mozilla.firefox";
    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String SAMPLE_APP = "com.okta.oidc.example";
    //timeout for app transition from browser to app.
    private static final int TRANSITION_TIMEOUT = 2000;
    private static final int NETWORK_TIMEOUT = 5000;

    //web page resource ids
    private static final String ID_USERNAME = "okta-signin-username";
    private static final String ID_PASSWORD = "okta-signin-password";
    private static final String ID_SUBMIT = "okta-signin-submit";
    private static final String ID_NO_THANKS = "com.android.chrome:id/negative_button";
    private static final String ID_ACCEPT = "com.android.chrome:id/terms_accept";
    private static final String ID_CLOSE_BROWSER = "com.android.chrome:id/close_button";
    private static final String ID_ADDRESS_BAR = "com.android.chrome:id/url_bar";

    //app resource ids
    private static final String ID_PROGRESS_BAR = "com.okta.oidc.example:id/progress_horizontal";

    private AuthenticationPayload mMockPayload;

    private Context mMockContext;

    private String mState;
    private String mNonce;

    private final String FAKE_CODE = "NPcg5pmx7oZbXSfbnhmE";

    private String mRedirect;
    private UiDevice mDevice;
    @Rule
    public ActivityTestRule<SampleActivity> activityRule = new ActivityTestRule<>(SampleActivity.class);
    @Rule
    public GrantPermissionRule grant = GrantPermissionRule.grant(READ_EXTERNAL_STORAGE, INTERNET);

    public WireMockServer mWireMockServer;

    private class MockConnectionFactory implements HttpConnectionFactory {
        @NonNull
        @Override
        public HttpURLConnection build(@NonNull URL url) throws IOException {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            @Override
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }
                });
            } catch (Exception e) {
                //NO-OP
            }
            return (HttpURLConnection) url.openConnection();
        }
    }

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(getInstrumentation());
        mMockContext = InstrumentationRegistry.getInstrumentation().getContext();
        mWireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .keystorePassword(KEYSTORE_PASSWORD)
                .keystoreType("BKS")
                .keystorePath(KEYSTORE_PATH)
                .port(PORT)
                .httpsPort(HTTPS_PORT));
        mWireMockServer.start();

        mState = CodeVerifierUtil.generateRandomState();
        mNonce = CodeVerifierUtil.generateRandomState();
        mMockPayload = new AuthenticationPayload.Builder()
                .setState(mState)
                .addParameter("nonce", mNonce)
                .build();
        mRedirect = String.format("com.oktapreview.samples-test:/callback?code=%s&state=%s", FAKE_CODE, mState);
        //samples sdk test
        activityRule.getActivity().mOidcConfig = new OIDCConfig.Builder()
                .clientId("0oajqehiy6p81NVzA0h7")
                .redirectUri("com.oktapreview.samples-test:/callback")
                .endSessionRedirectUri("com.oktapreview.samples-test:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://127.0.0.1:8443")
                .create();

        WebAuthClient mWebOktaAuth = new Okta.WebAuthBuilder()
                .withConfig(activityRule.getActivity().mOidcConfig)
                .withContext(activityRule.getActivity())
                .withStorage(new SimpleOktaStorage(activityRule.getActivity()))
                .withHttpConnectionFactory(new MockConnectionFactory())
                .create();

        activityRule.getActivity().mWebAuth = mWebOktaAuth;

        activityRule.getActivity().setupCallback();
    }

    @After
    public void tearDown() throws Exception {
        mWireMockServer.stop();
    }

    private UiObject getProgressBar() {
        return mDevice.findObject(new UiSelector().resourceId(ID_PROGRESS_BAR));
    }

    private void acceptChromePrivacyOption() throws UiObjectNotFoundException {
        UiSelector selector = new UiSelector();
        UiObject accept = mDevice.findObject(selector.resourceId(ID_ACCEPT));
        accept.waitForExists(TRANSITION_TIMEOUT);
        if (accept.exists()) {
            accept.click();
        }

        UiObject noThanks = mDevice.findObject(selector.resourceId(ID_NO_THANKS));
        noThanks.waitForExists(TRANSITION_TIMEOUT);
        if (noThanks.exists()) {
            noThanks.click();
        }
    }

    @Test
    public void test1_loginNoSession() throws UiObjectNotFoundException, InterruptedException {
        activityRule.getActivity().mPayload = mMockPayload;
        mockConfigurationRequest(aResponse()
                .withStatus(HTTP_OK)
                .withBody(getAsset(mMockContext, "configuration.json")));

        mockWebAuthorizeRequest(aResponse().withStatus(HTTP_MOVED_TEMP)
                .withHeader("Location", mRedirect));

        String tokenResponse = getAsset(mMockContext, "token_response.json");

        String jwt = Utils.getJwt(ISSUER, mNonce, getTomorrow(), getNow(),
                activityRule.getActivity().mOidcConfig.getClientId());

        String token = String.format(tokenResponse, jwt);

        mockTokenRequest(aResponse().withStatus(HTTP_OK)
                .withBody(token));

        onView(withId(R.id.sign_in_native)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        UiSelector selector = new UiSelector();
        UiObject address = mDevice.findObject(selector.resourceId(ID_ADDRESS_BAR));
        if (!address.exists()) {
            acceptChromePrivacyOption();
        }

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));

        verify(getRequestedFor(urlMatching("/authorize.*"))
                .withHeader(X_OKTA_USER_AGENT, equalTo(USER_AGENT_HEADER)));
    }
}