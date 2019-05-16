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

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.okta.oidc.AuthenticationPayload;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SampleActivityTest {
    //apk package names
    @SuppressWarnings("unused")
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
        if (Utils.isEmulator()) {
            activityRule.getActivity().mStorageOidc.requireHardwareKeyStore = false;
            activityRule.getActivity().mStorageOAuth2.requireHardwareKeyStore = false;
        }
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

    private void customTabInteraction(boolean enterUserName) throws UiObjectNotFoundException {
        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        acceptChromePrivacyOption();
        UiSelector selector = new UiSelector();
        if (enterUserName) {
            UiObject username = mDevice.findObject(selector.resourceId(ID_USERNAME));
            username.setText(USERNAME);
        }
        UiObject password = mDevice.findObject(selector.resourceId(ID_PASSWORD));
        password.setText(PASSWORD);
        UiObject signIn = mDevice.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
    }

    //Need this to keep signing in. if device doesn't support hardware encryption, data
    //is not persisted.
    private void signInIfNotAlready() {
        onView(withId(R.id.clear_data)).withFailureHandler((error, viewMatcher) -> {
            test3_signInWithSession();
        }).check(matches(isDisplayed()));
    }

    @Test
    public void test1_signInNoSession() throws UiObjectNotFoundException {
        onView(withId(R.id.switch1)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.switch1)).check(matches(isDisplayed()));
            onView(withId(R.id.switch1)).perform(click());
        }).check(matches(isChecked()));

        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        customTabInteraction(true);

        //check if get profile is visible
        getProfileButton().waitForExists(TRANSITION_TIMEOUT);
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }

    @Test
    public void test2_clearData() {
        signInIfNotAlready();
        onView(withId(R.id.clear_data)).perform(click());
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
    }

    @Test
    public void test3_signInWithSession() {
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        assertNotNull(activityRule.getActivity().mSessionClient.getTokens());
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));
    }

    @Test
    public void test4_introspect() {
        signInIfNotAlready();
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

    @Test
    public void test5_refreshToken() {
        signInIfNotAlready();
        onView(withId(R.id.refresh_token)).check(matches(isDisplayed()));
        onView(withId(R.id.refresh_token)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText("token refreshed")));
    }

    @Test
    public void test6_getProfile() {
        signInIfNotAlready();
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.get_profile)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString(USERNAME))));
    }

    @Test
    public void test7_revokeRefreshToken() {
        signInIfNotAlready();
        onView(withId(R.id.revoke_refresh)).check(matches(isDisplayed()));
        onView(withId(R.id.revoke_refresh)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    @Test
    public void test8_revokeAccessToken() {
        signInIfNotAlready();
        onView(withId(R.id.revoke_access)).check(matches(isDisplayed()));
        onView(withId(R.id.revoke_access)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    @Test
    public void test9_signOutOfOkta() {
        signInIfNotAlready();
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_out)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);
        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("signedOutOfOkta"))));
    }

    @Test
    public void testA_cancelSignIn() throws UiObjectNotFoundException {
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
    public void testB_signInWithPayload() throws UiObjectNotFoundException {
        activityRule.getActivity().mPayload = new AuthenticationPayload.Builder()
                .setLoginHint("devex@okta.com")
                .addParameter("max_age", "5000")
                .build();

        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));

        onView(withId(R.id.sign_in)).perform(click());

        customTabInteraction(false);

        //wait for token exchange
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);

        //check if get profile is visible
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }

    @Test
    public void testC_nativeLogIn() {
        assumeTrue(
                "Can only run on API Level 24 or later because AuthenticationAPI " +
                        "requires Java 8",
                Build.VERSION.SDK_INT >= 24
        );
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

    @Test
    public void testD_checkIfTokenExpired() {
        signInIfNotAlready();
        onView(withId(R.id.check_expired)).check(matches(isDisplayed()));
        onView(withId(R.id.check_expired)).perform(click());
        onView(withId(R.id.status))
                .check(matches(withText(containsString("token not expired"))));
    }

    @Test
    public void testE_OAuth2ResourceConfig() throws UiObjectNotFoundException {
        onView(withId(R.id.switch1)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.switch1)).check(matches(isDisplayed()));
            onView(withId(R.id.switch1)).perform(click());
        }).check(matches(isNotChecked()));

        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            test9_signOutOfOkta();
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));

        onView(withId(R.id.sign_in)).perform(click());
        //check if get profile is visible
        getProfileButton().waitForExists(TRANSITION_TIMEOUT);
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.get_profile)).perform(click());
        onView(withId(R.id.status))
                .check(matches(withText(
                        containsString("Profile not supported for OAuth resource"))));
    }


}