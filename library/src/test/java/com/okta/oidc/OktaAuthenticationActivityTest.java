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
package com.okta.oidc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;

import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import androidx.test.platform.app.InstrumentationRegistry;

import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_AUTH_URI;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_BROWSERS;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_TAB_OPTIONS;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaAuthenticationActivityTest {
    private static final String CHROME_STABLE = "com.android.chrome";
    private Context mContext;
    @Mock
    private Context mMockContext;
    private ActivityController<OktaAuthenticationActivity> mActivityController;
    private OktaAuthenticationActivity mActivity;
    private ShadowActivity mShadowActivity;
    @Captor
    private ArgumentCaptor<Intent> mConnectIntentCaptor;
    @Captor
    private ArgumentCaptor<CustomTabsServiceConnection> mConnectionCaptor;
    @Mock
    CustomTabsClient mClient;
    private Intent mStandardAuthorize;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mStandardAuthorize = new Intent(mContext, OktaAuthenticationActivity.class);
        mStandardAuthorize.putExtra(EXTRA_AUTH_URI, Uri.parse(CUSTOM_URL));

//        if (mSupportedBrowsers != null) {
//            intent.putExtra(EXTRA_BROWSERS, mSupportedBrowsers);
//        }
//        intent.putExtra(EXTRA_TAB_OPTIONS, mCustomTabColor);
//        if (mErrorActivityResult != null) {
//            intent.putExtra(EXTRA_EXCEPTION, mErrorActivityResult.toJsonString());
//            mErrorActivityResult = null;
//        }



    }

    @Test
    public void testAuthorize() {
        instantiateActivity(mStandardAuthorize);
        Mockito.doReturn(true).when(mMockContext).bindService(
                mConnectIntentCaptor.capture(),
                mConnectionCaptor.capture(),
                Mockito.anyInt());

        mActivityController.create().start().resume();
        provideClient();
        // check the service connection is made to the specified package
        Intent intent = mConnectIntentCaptor.getValue();
        assertThat(intent.getPackage()).isEqualTo(CHROME_STABLE);
    }

    private void provideClient() {
        CustomTabsServiceConnection conn = mConnectionCaptor.getValue();
        conn.onCustomTabsServiceConnected(
                new ComponentName(CHROME_STABLE, CHROME_STABLE + ".CustomTabsService"),
                mClient);
    }

    private void instantiateActivity(Intent intent) {
        mActivityController = Robolectric.buildActivity(
                OktaAuthenticationActivity.class,
                intent);

        mActivity = mActivityController.get();
        mShadowActivity = shadowOf(mActivity);
    }
}