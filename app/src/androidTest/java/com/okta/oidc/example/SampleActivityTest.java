package com.okta.oidc.example;

import android.text.TextUtils;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SampleActivityTest {
    private static final String FIRE_FOX = "org.mozilla.firefox";
    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String SAMPLE_APP = "com.okta.oidc.example";
    private static final int TIMEOUT = 5000;
    private static final String ID_USERNAME = "okta-signin-username";
    private static final String ID_PASSWORD = "okta-signin-password";
    private static final String ID_SUBMIT = "okta-signin-submit";
    private UiDevice mDevice;
    @Rule
    public ActivityTestRule<SampleActivity> activityRule = new ActivityTestRule<>(SampleActivity.class);

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        activityRule.getActivity().mOktaAuth.clear();
    }

    @Test
    public void loginWithoutExistingSession() throws UiObjectNotFoundException {
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TIMEOUT);
        UiSelector selector = new UiSelector();

        UiObject username = mDevice.findObject(selector.resourceId(ID_USERNAME));
        username.setText("devex@okta.com");

        UiObject password = mDevice.findObject(selector.resourceId(ID_PASSWORD));
        password.setText("Dev3xIsD0pe");

        UiObject signIn = mDevice.findObject(selector.resourceId(ID_SUBMIT));
        signIn.click();

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TIMEOUT);

        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
    }

    @Test
    public void clearData() {
        onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
        onView(withId(R.id.clear_data)).perform(click());
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        assertNull(activityRule.getActivity().mOktaAuth.getTokens());
    }

    @Test
    public void loginWithExistingSession() throws UiObjectNotFoundException {
        onView(withId(R.id.sign_in)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(CHROME_STABLE)), TIMEOUT);

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TIMEOUT);
        assertNotNull(activityRule.getActivity().mOktaAuth.getTokens());
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));
    }

    @Test
    public void signOutFromOkta() {
        onView(withId(R.id.sign_out)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_out)).perform(click());

        mDevice.wait(Until.findObject(By.pkg(SAMPLE_APP)), TIMEOUT);

        TextView status = activityRule.getActivity().findViewById(R.id.status);
        assertTrue(TextUtils.isEmpty(status.getText())); //should have no errors
    }

}