package com.okta.oidc.example;

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
    private static final String ID_PROGRESS_BAR = "com.okta.oidc.example:id/progress_horizontal";
    private static final String ID_CLOSE_BROWSER = "com.android.chrome:id/close_button";

    private static final String PASSWORD = "Dev3xIsD0pe";
    private static final String USERNAME = "devex@okta.com";

    private UiDevice mDevice;
    @Rule
    public ActivityTestRule<SampleActivity> activityRule = new ActivityTestRule<>(SampleActivity.class);

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(getInstrumentation());
        //activityRule.getActivity().mOktaAuth.clear();
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
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
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
    public void test3_loginWithSession() {
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TRANSITION_TIMEOUT);

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TRANSITION_TIMEOUT);
        assertNotNull(activityRule.getActivity().mOktaAuth.getTokens());
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));

        refreshToken();
        getProfile();
        revokeRefreshToken();
        revokeAccessToken();
    }
    
    private void refreshToken() {
        onView(withId(R.id.refresh_token)).check(matches(isDisplayed()));
        onView(withId(R.id.refresh_token)).perform(click());

        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText("token refreshed")));
    }

    private void getProfile() {
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.get_profile)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString(USERNAME))));
    }

    private void revokeRefreshToken() {
        onView(withId(R.id.revoke_refresh)).check(matches(isDisplayed()));
        onView(withId(R.id.revoke_refresh)).perform(click());
        //wait for network
        getProgressBar().waitUntilGone(NETWORK_TIMEOUT);
        onView(withId(R.id.status)).check(matches(withText(containsString("true"))));
    }

    private void revokeAccessToken() {
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
        onView(withId(R.id.status)).check(matches(withText(containsString("error"))));
    }

}