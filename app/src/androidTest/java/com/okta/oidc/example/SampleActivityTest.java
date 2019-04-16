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

import com.okta.oidc.AuthenticationPayload;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.core.StringContains.containsString;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SampleActivityTest {
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
    private static final String ID_GET_PROFILE = "com.okta.oidc.example:id/get_profile";

    private static String PASSWORD;
    private static String USERNAME;

    private UiDevice mDevice;
    @Rule
    public ActivityTestRule<SampleActivity> activityRule = new ActivityTestRule<>(SampleActivity.class);

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(getInstrumentation());
        USERNAME = BuildConfig.USERNAME;
        PASSWORD = BuildConfig.PASSWORD;

    }

    private UiObject getProgressBar() {
        return mDevice.findObject(new UiSelector().resourceId(ID_PROGRESS_BAR));
    }

    private UiObject getProfileButton() {
        return mDevice.findObject(new UiSelector().resourceId(ID_GET_PROFILE));
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
        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        acceptChromePrivacyOption();

        UiSelector selector = new UiSelector();

        UiObject username = mDevice.findObject(selector.resourceId(ID_USERNAME));
        username.setText(USERNAME);

        UiObject password = mDevice.findObject(selector.resourceId(ID_PASSWORD));
        password.setText(PASSWORD);

        UiObject signIn = mDevice.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        getProfileButton().waitForExists(TRANSITION_TIMEOUT);
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }

    @Test
    public void test2_clearData() {
        onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
        onView(withId(R.id.clear_data)).perform(click());
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
    }

    @Test
    public void test3_logInWithSession() {
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        assertNotNull(activityRule.getActivity().mOktaAuth.getTokens());
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));
    }

    @Test
    public void test4_introspect() {
        introspectAccessToken();
        introspectRefreshToken();
        introspectIdToken();
    }

    private void introspectAccessToken() {
        onView(withId(R.id.introspect_access)).check(matches(isDisplayed()));
        onView(withId(R.id.introspect_access)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    private void introspectRefreshToken() {
        onView(withId(R.id.introspect_refresh)).check(matches(isDisplayed()));
        onView(withId(R.id.introspect_refresh)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    private void introspectIdToken() {
        onView(withId(R.id.introspect_id)).check(matches(isDisplayed()));
        onView(withId(R.id.introspect_id)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    public void test5_refreshToken() {
        onView(withId(R.id.refresh_token)).check(matches(isDisplayed()));
        onView(withId(R.id.refresh_token)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText("token refreshed")));
    }

    public void test6_getProfile() {
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.get_profile)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString(USERNAME))));
    }

    public void test7_revokeRefreshToken() {
        onView(withId(R.id.revoke_refresh)).check(matches(isDisplayed()));
        onView(withId(R.id.revoke_refresh)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    public void test8_revokeAccessToken() {
        onView(withId(R.id.revoke_access)).check(matches(isDisplayed()));
        onView(withId(R.id.revoke_access)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    @Test
    public void test8_signOutFromOkta() {
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_out)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("signedOutFromOkta"))));
    }

    @Test
    public void test9_cancelSignIn() throws UiObjectNotFoundException {
        onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
        onView(withId(R.id.clear_data)).perform(click());
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));

        onView(withId(R.id.sign_in)).perform(click());
        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        UiSelector selector = new UiSelector();
        UiObject closeBrowser = mDevice.findObject(selector.resourceId(ID_CLOSE_BROWSER));
        closeBrowser.click();

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("canceled"))));
    }

    @Test
    public void testA_logInWithPayload() throws UiObjectNotFoundException {
        activityRule.getActivity().mPayload = new AuthenticationPayload.Builder()
                .setLoginHint("devex@okta.com")
                .addParameter("max_age", "5000")
                .build();

        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));

        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        acceptChromePrivacyOption();

        UiSelector selector = new UiSelector();

        UiObject password = mDevice.findObject(selector.resourceId(ID_PASSWORD));
        password.setText(PASSWORD);

        UiObject signIn = mDevice.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }

    @Test
    public void testB_nativeLogIn() {
        onView(withId(R.id.sign_in_native)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_native)).perform(click());

        onView(withId(R.id.username)).check(matches(isDisplayed()));
        onView(withId(R.id.username)).perform(click(), replaceText(BuildConfig.USERNAME));

        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).perform(click(), replaceText(BuildConfig.PASSWORD));

        onView(withId(R.id.submit)).check(matches(isDisplayed()));
        onView(withId(R.id.submit)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        //check if get profile is visible
        getProfileButton().waitForExists(TRANSITION_TIMEOUT);
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }
}