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

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.util.CodeVerifierUtil;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.okta.oidc.example.Utils.getAsset;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.core.StringContains.containsString;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WireMockTest {
    private static final String DEFAULT_WIREMOCK_ADDRESS = "https://localhost";
    private static final String KEYSTORE_DIRECTORY =
            "src/test/resources/wiremock/keystore/mock.keystore.jks";
    private static final String CONFIG_DIRECTORY = "src/test/resources/wiremock/";
    private static final String KEYSTORE_PASSWORD = "123456";
    private static final int PORT = 1010;

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
    public WireMockClassRule instanceRule = new WireMockClassRule(options().needClientAuth(false));

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(getInstrumentation());
        mMockContext = InstrumentationRegistry.getInstrumentation().getContext();
        mState = CodeVerifierUtil.generateRandomState();
        mNonce = CodeVerifierUtil.generateRandomState();
        mMockPayload = new AuthenticationPayload.Builder()
                .setState(mState)
                .addParameter("nonce", mNonce)
                .build();
        mRedirect = String.format("com.oktapreview.samples-test:/callback?code=%s&state=%s", FAKE_CODE, mState);
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
    public void test1_loginNoSession() throws UiObjectNotFoundException {
        activityRule.getActivity().mPayload = mMockPayload;
        Utils.mockConfigurationRequest(aResponse()
                .withStatus(HTTP_OK)
                .withBody(getAsset(mMockContext, "configuration.json")));

        Utils.mockWebAuthorizeRequest(aResponse().withStatus(HTTP_MOVED_TEMP)
                .withHeader("Location", mRedirect));

        Utils.mockTokenRequest(aResponse().withStatus(HTTP_OK)
                .withBody(getAsset(mMockContext, "token_response.json")));

        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        acceptChromePrivacyOption();

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));


    }

}