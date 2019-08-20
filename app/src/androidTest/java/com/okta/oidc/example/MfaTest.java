package com.okta.oidc.example;

import android.os.RemoteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.okta.oidc.example.Utils.EXCHANGE_TIMEOUT;
import static com.okta.oidc.example.Utils.RECENTS_TIMEOUT;
import static com.okta.oidc.example.Utils.TEXT_APPROVE;
import static com.okta.oidc.example.Utils.TEXT_DENY;
import static com.okta.oidc.example.Utils.TEXT_SEND_PUSH;
import static com.okta.oidc.example.Utils.NOTIFICATION_TIMEOUT;
import static com.okta.oidc.example.Utils.TRANSITION_TIMEOUT;
import static com.okta.oidc.example.Utils.WAIT_FOR_POLL_TIMEOUT;
import static com.okta.oidc.example.Utils.customTabInteractionWithMfa;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.StringContains.containsString;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MfaTest {
    //app resource ids
    private static final String ID_GET_PROFILE = "com.okta.oidc.example:id/get_profile";
    private static final String ID_CLEAR_ALL = "com.android.systemui:id/dismiss_text";
    private static final String OKTA_OIDC_APP = "Okta OIDC Plain";
    private static final String OKTA_VERIFY = "com.okta.android.auth";
    private static final String NEXUS_LAUNCHER = "com.google.android.apps.nexuslauncher";
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String ID_TITLE = "android:id/title";
    private static final String CLASS_TEXTVIEW = "android.widget.TextView";
    private static final String NOTIFICATION = "MFA Notification";
    private static final String DENY_TEXTVIEW = "You have chosen to reject this login.";
    private static final int STEPS = 10;

    private UiDevice mDevice;
    @Rule
    public ActivityTestRule<PlainActivity> activityRule = new ActivityTestRule<>(PlainActivity.class);

    private UiObject getProfileButton() {
        return mDevice.findObject(new UiSelector().resourceId(ID_GET_PROFILE));
    }

    private UiObject getSendPushButton() {
        return mDevice.findObject(new UiSelector().text(TEXT_SEND_PUSH));
    }

    private UiObject getApproveButton() {
        return mDevice.findObject(new UiSelector().description(TEXT_APPROVE));
    }

    private UiObject getDenyButton() {
        return mDevice.findObject(new UiSelector().description(TEXT_DENY));
    }

    private void clearAllNotifications() {
        mDevice.openNotification();
        mDevice.wait(Until.hasObject(By.pkg(SYSTEM_UI)), TRANSITION_TIMEOUT);
        UiObject2 clearAll = mDevice.findObject(By.res(ID_CLEAR_ALL));
        if (clearAll != null) {
            clearAll.click();
        } else {
            mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight(),
                    mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight() / 2, STEPS);
        }
    }

    private UiObject waitForNotification() {
        mDevice.openNotification();
        mDevice.wait(Until.hasObject(By.pkg(SYSTEM_UI)), TRANSITION_TIMEOUT);
        UiObject notification = mDevice.findObject(
                new UiSelector().resourceId(ID_TITLE)
                        .className(CLASS_TEXTVIEW)
                        .packageName(SYSTEM_UI)
                        .textContains(NOTIFICATION));
        notification.waitForExists(NOTIFICATION_TIMEOUT);
        return notification;
    }

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(getInstrumentation());
        clearAllNotifications();
    }

    @Test
    public void signInApproveViaNotification() throws UiObjectNotFoundException {
        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        //sign in custom tab with mfa account
        customTabInteractionWithMfa(mDevice, true);
        //perform mfa with push
        UiObject sendPush = getSendPushButton();
        sendPush.click();

        //wait for push notification to show
        waitForNotification();
        UiObject approve = getApproveButton();
        approve.click();

        //wait for token exchange to see if profile button shows
        getProfileButton().waitForExists(EXCHANGE_TIMEOUT);
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }

    @Test
    public void signInDenyViaNotification() throws UiObjectNotFoundException {
        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        //sign in custom tab with mfa account
        customTabInteractionWithMfa(mDevice, true);
        //perform mfa with push
        UiObject sendPush = getSendPushButton();
        sendPush.click();

        //wait for push notification to show
        waitForNotification();
        UiObject deny = getDenyButton();
        deny.click();

        UiObject denyText = mDevice.findObject(new UiSelector().text(DENY_TEXTVIEW));
        //wait for deny notification to show
        denyText.waitForExists(EXCHANGE_TIMEOUT);
        assertTrue(denyText.exists());
    }

    @Test
    public void signInApproveViaOktaVerify() throws UiObjectNotFoundException, InterruptedException, RemoteException {
        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        //sign in custom tab with mfa account
        customTabInteractionWithMfa(mDevice, true);
        //perform mfa with push
        UiObject sendPush = getSendPushButton();
        sendPush.click();

        //wait for push notification to show
        UiObject notification = waitForNotification();
        assertTrue(notification.exists());
        notification.click();
        mDevice.wait(Until.hasObject(By.pkg(OKTA_VERIFY).depth(0)), TRANSITION_TIMEOUT);
        //wait for chrome background polling to finish.
        Thread.sleep(WAIT_FOR_POLL_TIMEOUT);

        mDevice.findObject(new UiSelector().text(TEXT_APPROVE)).click();
        //Okta Verify kicks out to launcher
        mDevice.wait(Until.hasObject(By.pkg(NEXUS_LAUNCHER).depth(0)), TRANSITION_TIMEOUT);
        //home and back again
        mDevice.pressHome();
        //press twice to go back to app
        mDevice.pressRecentApps();
        Thread.sleep(RECENTS_TIMEOUT);
        mDevice.pressRecentApps();
        //wait for token exchange to see if profile button shows
        getProfileButton().waitForExists(EXCHANGE_TIMEOUT);
        onView(withId(R.id.get_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.status))
                .check(matches(withText(containsString("authentication authorized"))));
    }

    @Test
    public void signInDenyViaOktaVerify() throws UiObjectNotFoundException, InterruptedException, RemoteException {
        onView(withId(R.id.sign_in)).withFailureHandler((error, viewMatcher) -> {
            onView(withId(R.id.clear_data)).check(matches(isDisplayed()));
            onView(withId(R.id.clear_data)).perform(click());
        }).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in)).perform(click());

        //sign in custom tab with mfa account
        customTabInteractionWithMfa(mDevice, true);
        //perform mfa with push
        UiObject sendPush = getSendPushButton();
        sendPush.click();

        //wait for push notification to show
        UiObject notification = waitForNotification();
        assertTrue(notification.exists());
        notification.click();
        mDevice.wait(Until.hasObject(By.pkg(OKTA_VERIFY).depth(0)), TRANSITION_TIMEOUT);
        //wait for chrome background polling to finish.
        Thread.sleep(WAIT_FOR_POLL_TIMEOUT);

        mDevice.findObject(new UiSelector().text(TEXT_DENY)).click();
        //Okta Verify kicks out to launcher
        mDevice.wait(Until.hasObject(By.pkg(NEXUS_LAUNCHER).depth(0)), TRANSITION_TIMEOUT);
        //home and back again
        mDevice.pressHome();
        //press twice to go back to app
        mDevice.pressRecentApps();
        Thread.sleep(RECENTS_TIMEOUT);
        mDevice.pressRecentApps();

        //Look for denied text.
        UiObject denyText = mDevice.findObject(new UiSelector().text(DENY_TEXTVIEW));
        //wait for deny notification to show
        denyText.waitForExists(TRANSITION_TIMEOUT);
        assertTrue(denyText.exists());
    }
}
