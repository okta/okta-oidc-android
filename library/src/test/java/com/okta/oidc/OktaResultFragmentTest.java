package com.okta.oidc;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.okta.oidc.net.request.ProviderConfiguration;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.platform.app.InstrumentationRegistry;

import static android.app.Activity.RESULT_OK;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaResultFragmentTest {
    private static final String CUSTOM_STATE = "CUSTOM_STATE";
    private static final String ERROR = "ANY_ERROR";
    private Context mContext;
    private MockEndPoint mEndPoint;
    private OIDCConfig mAccount;
    private ProviderConfiguration mProviderConfig;
    private FragmentActivity mActivity;

    @Mock
    OktaResultFragment.AuthResultListener listener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
        mProviderConfig = TestValues.getProviderConfiguration(url);
        mActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
    }

    private OktaResultFragment getOktaResultFragment(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(OktaResultFragment.AUTHENTICATION_REQUEST);
        if (fragment == null) {
            return null;
        }
        return (OktaResultFragment) fragment;
    }


    @Test
    public void handleAuthorizationResponseLoginSuccess() {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?state=" + CUSTOM_STATE));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.AUTHORIZED);
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_IN);
    }

    @Test
    public void handleAuthorizationResponseLoginFailed() {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?error=" + ERROR));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_IN);
        assert (ERROR.equalsIgnoreCase(resultCapture.getValue().getException().error));
    }

    @Test
    public void handleAuthorizationResponseLogoutSuccess() {
        OktaResultFragment.addLogoutFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?state=" + CUSTOM_STATE));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.LOGGED_OUT);
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_OUT);
    }

    @Test
    public void handleAuthorizationResponseLogoutFailed() {
        OktaResultFragment.addLogoutFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?error=" + ERROR));

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (getOktaResultFragment(mActivity) == null);
        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (ERROR.equalsIgnoreCase(resultCapture.getValue().getException().error));
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_OUT);
    }

    @Test
    public void handleAuthorizationResponseWithEmptyIntent() {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.putExtra("RANDOM_KEY", "RANDOM_VALUE");

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());


        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_IN);
        assert (AuthorizationException.AuthorizationRequestErrors.OTHER.code == resultCapture.getValue().getException().code);
        assert (getOktaResultFragment(mActivity) == null);
    }

    @Test
    public void handleAuthorizationResponseWithInvalidJsonErrorInIntent() {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXCEPTION, "RANDOM_VALUE");

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_IN);
        assert (AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR.code == resultCapture.getValue().getException().code);
        assert (getOktaResultFragment(mActivity) == null);
    }

    @Test
    public void handleAuthorizationResponseWithValidJsonErrorInIntent() {
        OktaResultFragment.addLoginFragment(TestValues.getAuthorizeRequest(mAccount, null), 0, mActivity, listener, new String[]{});

        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXCEPTION, TestValues.getAuthorizationExceptionError());

        getOktaResultFragment(mActivity).onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        ArgumentCaptor<OktaResultFragment.ResultType> resultTypeCapture = ArgumentCaptor.forClass(OktaResultFragment.ResultType.class);
        verify(listener).postResult(resultCapture.capture(), resultTypeCapture.capture());

        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (resultTypeCapture.getValue() == OktaResultFragment.ResultType.SIGN_IN);
        assert (AuthorizationException.TYPE_GENERAL_ERROR == resultCapture.getValue().getException().type);
        assert (getOktaResultFragment(mActivity) == null);
    }

}
