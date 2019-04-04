package com.okta.oidc.example;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiDevice;
//import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SampleActivityTest {
    private UiDevice mDevice;
    @Rule
    public IntentsTestRule<SampleActivity> mIntentsRule = new IntentsTestRule<>(
            SampleActivity.class);

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance();

    }

    @Test
    public void loginCancel() {
    }
}