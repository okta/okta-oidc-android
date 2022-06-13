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

import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static android.app.Activity.RESULT_OK;
import static com.okta.oidc.AuthenticationResultHandler.handler;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaResultFragmentTest {
    private static final String CUSTOM_STATE = "CUSTOM_STATE";
    private static final String ERROR = "ANY_ERROR";
    private MockEndPoint mEndPoint;
    private OIDCConfig mConfig;
    private FragmentActivity mActivity;
    private CustomTabOptions mCustomTabOptions = new CustomTabOptions();

    @Mock
    AuthenticationResultHandler.AuthResultListener listener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mConfig = TestValues.getConfigWithUrl(url);
        mActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
    }

    private OktaResultFragment getOktaResultFragment(FragmentActivity activity) {
        ShadowLooper.runUiThreadTasks();
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(OktaResultFragment.AUTHENTICATION_REQUEST);
        if (fragment == null) {
            return null;
        }
        return (OktaResultFragment) fragment;
    }


    @Test
    public void handleAuthorizationResponseLoginSuccess() throws AuthorizationException {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?state=" + CUSTOM_STATE));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.AUTHORIZED);
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_IN);
    }

    @Test
    public void handleAuthorizationResponseLoginFailed() throws AuthorizationException {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?error=" + ERROR));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.ERROR);
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_IN);
        assert (ERROR.equalsIgnoreCase(resultCapture.getValue().getException().error));
    }

    @Test
    public void handleAuthorizationResponseLogoutSuccess() throws AuthorizationException {
        OktaResultFragment.addLogoutFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?state=" + CUSTOM_STATE));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.LOGGED_OUT);
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_OUT);
    }

    @Test
    public void handleAuthorizationResponseLogoutFailed() throws AuthorizationException {
        OktaResultFragment.addLogoutFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?error=" + ERROR));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.ERROR);
        assert (ERROR.equalsIgnoreCase(resultCapture.getValue().getException().error));
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_OUT);
    }

    @Test
    public void handleAuthorizationResponseWithEmptyIntent() throws AuthorizationException {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.putExtra("RANDOM_KEY", "RANDOM_VALUE");

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());


        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.ERROR);
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_IN);
        assert (AuthorizationException.AuthorizationRequestErrors.OTHER.code == resultCapture.getValue().getException().code);
        assert (getOktaResultFragment(mActivity) == null);
    }

    @Test
    public void handleAuthorizationResponseWithInvalidJsonErrorInIntent() throws AuthorizationException {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXCEPTION, "RANDOM_VALUE");

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.ERROR);
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_IN);
        assert (AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR.code == resultCapture.getValue().getException().code);
        assert (getOktaResultFragment(mActivity) == null);
    }

    @Test
    public void handleAuthorizationResponseWithValidJsonErrorInIntent() throws AuthorizationException {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mConfig, null), mCustomTabOptions, mActivity, new String[]{});
        handler().setAuthenticationListener(listener);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXCEPTION, TestValues.getAuthorizationExceptionError());

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<AuthenticationResultHandler.StateResult> resultCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.StateResult.class);
        ArgumentCaptor<AuthenticationResultHandler.ResultType> resultTypeCapture = ArgumentCaptor.forClass(AuthenticationResultHandler.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (resultCapture.getValue().getStatus() == AuthenticationResultHandler.Status.ERROR);
        assert (resultTypeCapture.getValue() == AuthenticationResultHandler.ResultType.SIGN_IN);
        assert (AuthorizationException.TYPE_GENERAL_ERROR == resultCapture.getValue().getException().type);
        assert (getOktaResultFragment(mActivity) == null);
    }

}
