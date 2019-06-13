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

import com.okta.oidc.AuthenticationResultHandler.AuthResultListener;
import com.okta.oidc.AuthenticationResultHandler.ResultType;
import com.okta.oidc.AuthenticationResultHandler.StateResult;
import com.okta.oidc.AuthenticationResultHandler.Status;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.net.response.web.LogoutResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.AuthorizationException.AuthorizationRequestErrors;
import com.okta.oidc.util.TestValues;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.okta.oidc.AuthenticationResultHandler.Status.CANCELED;
import static com.okta.oidc.AuthenticationResultHandler.Status.LOGGED_OUT;
import static com.okta.oidc.OktaAuthenticationActivity.EXTRA_EXCEPTION;
import static com.okta.oidc.OktaResultFragment.REQUEST_CODE_SIGN_IN;
import static com.okta.oidc.OktaResultFragment.REQUEST_CODE_SIGN_OUT;
import static com.okta.oidc.util.AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR;
import static com.okta.oidc.util.TestValues.CUSTOM_CODE;
import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AuthenticationResultHandlerTest {

    @Test
    public void handler() {
        AuthenticationResultHandler handler1 = AuthenticationResultHandler.handler();
        assertNotNull(handler1);
        AuthenticationResultHandler handler2 = AuthenticationResultHandler.handler();
        assertNotNull(handler2);
        assertEquals(handler1, handler2);
    }

    @Test
    public void setAuthenticationListener() throws InterruptedException {
        AuthenticationResultHandler.handler().mCachedResult = StateResult.canceled();
        AuthenticationResultHandler.handler().mCachedResultType = ResultType.SIGN_IN;

        final StateResult[] resultFromListener = new StateResult[1];
        final ResultType[] typeFromListener = new ResultType[1];
        CountDownLatch latch = new CountDownLatch(1);
        AuthResultListener listener = (result, type) -> {
            resultFromListener[0] = result;
            typeFromListener[0] = type;
            latch.countDown();
        };
        AuthenticationResultHandler.handler().setAuthenticationListener(listener);
        latch.await();
        assertEquals(resultFromListener[0].getStatus(), CANCELED);
        assertEquals(typeFromListener[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultSignInSuccess() throws InterruptedException {
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/callback?code=" + CUSTOM_CODE + "&state=" + CUSTOM_STATE));
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_IN, RESULT_OK, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        AuthorizeResponse response = (AuthorizeResponse) stateResult[0].getAuthorizationResponse();
        assertNotNull(response);
        assertEquals(stateResult[0].getStatus(), Status.AUTHORIZED);
        assertEquals(response.getState(), CUSTOM_STATE);
        assertEquals(response.getCode(), CUSTOM_CODE);
        assertEquals(stateType[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultSignInFailed() throws InterruptedException {
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?error=" + TestValues.ERROR));
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_IN, RESULT_OK, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), Status.ERROR);
        assertEquals(stateResult[0].getException().error, TestValues.ERROR);
        assertEquals(stateType[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultSignOutSuccess() throws InterruptedException {
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/logout?state=" + CUSTOM_STATE));
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        LogoutResponse response = (LogoutResponse) stateResult[0].getAuthorizationResponse();
        assertNotNull(response);
        assertEquals(stateResult[0].getStatus(), LOGGED_OUT);
        assertEquals(response.getState(), CUSTOM_STATE);
        assertEquals(stateType[0], ResultType.SIGN_OUT);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultSignOutFailed() throws InterruptedException {
        Intent intent = new Intent();
        intent.setData(Uri.parse("com.okta.test:/authorize?error=" + TestValues.ERROR));
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_OUT, RESULT_OK, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), Status.ERROR);
        assertEquals(stateResult[0].getException().error, TestValues.ERROR);
        assertEquals(stateType[0], ResultType.SIGN_OUT);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }


    @Test
    public void onActivityResultSignInCanceled() throws InterruptedException {
        Intent intent = new Intent();
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_IN, RESULT_CANCELED, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), CANCELED);
        assertEquals(stateType[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultSignOutCanceled() throws InterruptedException {
        Intent intent = new Intent();
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_OUT, RESULT_CANCELED, intent);
        latch.await();
        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), CANCELED);
        assertEquals(stateType[0], ResultType.SIGN_OUT);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultEmptyIntent() throws InterruptedException {
        Intent intent = new Intent();
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_IN, RESULT_OK, intent);
        latch.await();

        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), Status.ERROR);
        assertEquals(stateResult[0].getException().code, AuthorizationRequestErrors.OTHER.code);
        assertEquals(stateType[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultInvalidJson() throws InterruptedException {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXCEPTION, "invalid json");
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_IN, RESULT_OK, intent);
        latch.await();

        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), Status.ERROR);
        assertEquals(stateResult[0].getException().code, JSON_DESERIALIZATION_ERROR.code);
        assertEquals(stateType[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }

    @Test
    public void onActivityResultException() throws InterruptedException {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXCEPTION, TestValues.getAuthorizationExceptionError());
        CountDownLatch latch = new CountDownLatch(1);
        final StateResult[] stateResult = new StateResult[1];
        final ResultType[] stateType = new ResultType[1];
        AuthenticationResultHandler.handler().setAuthenticationListener((result, type) -> {
            stateResult[0] = result;
            stateType[0] = type;
            latch.countDown();
        });
        AuthenticationResultHandler.handler().onActivityResult(REQUEST_CODE_SIGN_IN, RESULT_OK, intent);
        latch.await();

        assertNotNull(stateResult[0]);
        assertEquals(stateResult[0].getStatus(), Status.ERROR);
        assertEquals(stateResult[0].getException().type, AuthorizationException.TYPE_GENERAL_ERROR);
        assertEquals(stateType[0], ResultType.SIGN_IN);
        assertNull(AuthenticationResultHandler.handler().mCachedResult);
        assertNull(AuthenticationResultHandler.handler().mCachedResultType);
    }
}