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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.okta.oidc.util.AuthorizationException;

import org.assertj.android.api.Assertions;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_AUTH_URI;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_BROWSERS;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_TAB_OPTIONS;
import static com.okta.oidc.OktaRedirectActivity.REDIRECT_ACTION;
import static com.okta.oidc.util.JsonStrings.FIRE_FOX;
import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaAuthClientActivityTest {
    private Context mContext;
    private ActivityController<OktaAuthenticationActivityMock> mActivityController;
    private OktaAuthenticationActivityMock mActivity;
    private ShadowActivity mShadowActivity;

    private Intent mAuthorizeSuccess;
    private Intent mAuthorizeSuccessWithExtras;
    private Intent mAuthorizeWithException;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mAuthorizeSuccess = new Intent(mContext, OktaAuthenticationActivityMock.class);
        mAuthorizeSuccess.putExtra(EXTRA_AUTH_URI, Uri.parse(CUSTOM_URL));

        mAuthorizeSuccessWithExtras = new Intent(mContext, OktaAuthenticationActivityMock.class);
        mAuthorizeSuccessWithExtras.putExtra(EXTRA_AUTH_URI, Uri.parse(CUSTOM_URL));
        mAuthorizeSuccessWithExtras.putExtra(EXTRA_BROWSERS, new String[]{FIRE_FOX});
        CustomTabOptions customTabOptions = new CustomTabOptions();
        customTabOptions.setCustomTabColor(100);
        customTabOptions.setStartEnterResId(200);
        customTabOptions.setStartExitResId(201);
        customTabOptions.setEndEnterResId(202);
        customTabOptions.setEndExitResId(203);

        mAuthorizeSuccessWithExtras.putExtra(EXTRA_TAB_OPTIONS, customTabOptions);

        mAuthorizeWithException = new Intent(mContext, OktaAuthenticationActivityMock.class);
        mAuthorizeWithException.putExtra(EXTRA_EXCEPTION,
                AuthorizationException.GeneralErrors.NETWORK_ERROR.toJsonString());

    }

    @Test
    public void testAuthorizeSuccess() {
        instantiateActivity(mAuthorizeSuccess);
        mActivityController.create().start().resume();

        Assertions.assertThat(mShadowActivity.getNextStartedActivity())
                .hasData(Uri.parse(CUSTOM_URL));

        Intent intent = new Intent(mContext, OktaAuthenticationActivityMock.class);
        intent.setAction(REDIRECT_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        mActivityController.newIntent(intent);

        assertThat(mShadowActivity.getResultCode()).isEqualTo(RESULT_OK);
        Assertions.assertThat(mActivity).isFinishing();
    }

    @Test
    public void testAuthorizeSuccessWithExtras() {
        instantiateActivity(mAuthorizeSuccessWithExtras);
        mActivityController.create().start().resume();

        Assertions.assertThat(mShadowActivity.getNextStartedActivity())
                .hasData(Uri.parse(CUSTOM_URL));

        assertTrue(mActivityController.get().mPreferredBrowsers.contains(FIRE_FOX));
        assertEquals(mActivityController.get().mCustomTabOptions.getCustomTabColor(), 100);
        assertEquals(mActivityController.get().mCustomTabOptions.getStartEnterResId(), 200);
        assertEquals(mActivityController.get().mCustomTabOptions.getStartExitResId(), 201);
        assertEquals(mActivityController.get().mCustomTabOptions.getEndEnterResId(), 202);
        assertEquals(mActivityController.get().mCustomTabOptions.getEndExitResId(), 203);

        Intent intent = new Intent(mContext, OktaAuthenticationActivityMock.class);
        intent.setAction(REDIRECT_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        mActivityController.newIntent(intent);

        assertThat(mShadowActivity.getResultCode()).isEqualTo(RESULT_OK);
        Assertions.assertThat(mActivity).isFinishing();
    }

    @Test
    public void testAuthorizeCancel() {
        instantiateActivity(mAuthorizeSuccess);
        mActivityController.create().start().resume();

        Assertions.assertThat(mShadowActivity.getNextStartedActivity())
                .hasData(Uri.parse(CUSTOM_URL));

        mActivityController.pause().resume();

        assertThat(mShadowActivity.getResultCode()).isEqualTo(RESULT_CANCELED);
        Assertions.assertThat(mActivity).isFinishing();
    }

    @Test
    public void testWithException() throws JSONException {
        instantiateActivity(mAuthorizeWithException);
        mActivityController.create();
        Bundle bundle = mActivityController.get().getIntent().getExtras();
        assertNotNull(bundle);
        String exception = bundle.getString(EXTRA_EXCEPTION);
        assertNotNull(exception);
        AuthorizationException ex = AuthorizationException.fromJson(exception);
        assertNotNull(ex);
        assertEquals(ex, AuthorizationException.GeneralErrors.NETWORK_ERROR);
        assertThat(mShadowActivity.getResultCode()).isEqualTo(RESULT_CANCELED);
    }


    private void instantiateActivity(Intent intent) {
        mActivityController = Robolectric.buildActivity(
                OktaAuthenticationActivityMock.class,
                intent);

        mActivity = mActivityController.get();
        mShadowActivity = shadowOf(mActivity);
    }
}