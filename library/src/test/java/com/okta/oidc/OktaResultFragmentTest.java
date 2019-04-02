package com.okta.oidc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;

import com.okta.oidc.net.request.ProviderConfiguration;
import com.okta.oidc.net.request.web.AuthorizeRequest;
import com.okta.oidc.net.request.web.WebRequest;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockResultCallback;
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

import androidx.test.platform.app.InstrumentationRegistry;

import static android.app.Activity.RESULT_OK;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaResultFragmentTest {
    private static final String CUSTOM_STATE = "CUSTOM_STATE";
    private static final String ERROR = "ANY_ERROR";
    private Context mContext;
    private MockEndPoint mEndPoint;
    private OIDCAccount mAccount;
    private ProviderConfiguration mProviderConfig;

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
    }


    @Test
    public void handleAuthorizationResponseLoginSuccess() {
        WebRequest request = new AuthorizeRequest.Builder()
                .account(mAccount)
                .providerConfiguration(mProviderConfig)
                .create();

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();

        OktaResultFragment.createLoginFragment(request, 0, fragmentActivity, listener, new String[]{});
        OktaResultFragment resultFragment = (OktaResultFragment) fragmentActivity.getSupportFragmentManager()
                .findFragmentByTag(OktaResultFragment.AUTHENTICATION_REQUEST);

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?state="+CUSTOM_STATE));

        resultFragment.onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        verify(listener).postResult(resultCapture.capture());

        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.AUTHORIZED);
    }

    @Test
    public void handleAuthorizationResponseLoginFailed() {
        WebRequest request = new AuthorizeRequest.Builder()
                .account(mAccount)
                .providerConfiguration(mProviderConfig)
                .create();

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();

        OktaResultFragment.createLoginFragment(request, 0, fragmentActivity, listener, new String[]{});
        OktaResultFragment resultFragment = (OktaResultFragment) fragmentActivity.getSupportFragmentManager()
                .findFragmentByTag(OktaResultFragment.AUTHENTICATION_REQUEST);

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?error="+ERROR));

        resultFragment.onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_IN, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        verify(listener).postResult(resultCapture.capture());

        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (ERROR.equalsIgnoreCase(resultCapture.getValue().getException().error));
    }

    @Test
    public void handleAuthorizationResponseLogoutSuccess() {
        WebRequest request = new AuthorizeRequest.Builder()
                .account(mAccount)
                .providerConfiguration(mProviderConfig)
                .create();

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();

        OktaResultFragment.createLogoutFragment(request, 0, fragmentActivity, listener, new String[]{});
        OktaResultFragment resultFragment = (OktaResultFragment) fragmentActivity.getSupportFragmentManager()
                .findFragmentByTag(OktaResultFragment.AUTHENTICATION_REQUEST);

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?state=" + CUSTOM_STATE));

        resultFragment.onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        verify(listener).postResult(resultCapture.capture());

        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.LOGGED_OUT);
    }

    @Test
    public void handleAuthorizationResponseLogoutFailed() {
        WebRequest request = new AuthorizeRequest.Builder()
                .account(mAccount)
                .providerConfiguration(mProviderConfig)
                .create();

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();

        OktaResultFragment.createLogoutFragment(request, 0, fragmentActivity, listener, new String[]{});
        OktaResultFragment resultFragment = (OktaResultFragment) fragmentActivity.getSupportFragmentManager()
                .findFragmentByTag(OktaResultFragment.AUTHENTICATION_REQUEST);

        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?error=" + ERROR));

        resultFragment.onActivityResult(OktaResultFragment.REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);

        ArgumentCaptor<OktaResultFragment.Result> resultCapture = ArgumentCaptor.forClass(OktaResultFragment.Result.class);
        verify(listener).postResult(resultCapture.capture());

        assert (resultCapture.getValue().getStatus() == OktaResultFragment.Status.ERROR);
        assert (ERROR.equalsIgnoreCase(resultCapture.getValue().getException().error));
    }

}
