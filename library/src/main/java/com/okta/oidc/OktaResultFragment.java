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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.okta.oidc.net.request.web.WebRequest;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class OktaResultFragment extends Fragment {
    static final String AUTHENTICATION_REQUEST = "authRequest";

    public static final int REQUEST_CODE_SIGN_IN = 100;
    public static final int REQUEST_CODE_SIGN_OUT = 200;
    private Intent authIntent;
    private Intent logoutIntent;

    public static void addLoginFragment(WebRequest request,
                                        CustomTabOptions customTabOptions,
                                        FragmentActivity activity,
                                        String[] browsers) {
        OktaResultFragment fragment = new OktaResultFragment();
        fragment.authIntent = createAuthIntent(activity, request.toUri(),
                customTabOptions, browsers);

        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, AUTHENTICATION_REQUEST)
                .commit();
    }

    public static void addLogoutFragment(WebRequest request,
                                         CustomTabOptions customTabOptions,
                                         FragmentActivity activity,
                                         String[] browsers) {
        OktaResultFragment fragment = new OktaResultFragment();
        fragment.logoutIntent = createAuthIntent(activity, request.toUri(), customTabOptions,
                browsers);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (!fragmentManager.isDestroyed()) {
            fragmentManager.beginTransaction()
                           .add(fragment, AUTHENTICATION_REQUEST)
                           .commit();
        }
    }

    @Override
    public void onResume() {
        if (authIntent != null) {
            startActivityForResult(authIntent, REQUEST_CODE_SIGN_IN);
            authIntent = null;
        }
        if (logoutIntent != null) {
            startActivityForResult(logoutIntent, REQUEST_CODE_SIGN_OUT);
            logoutIntent = null;
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static boolean hasRequestInProgress(FragmentActivity activity) {
        return activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST) != null;
    }

    public static OktaResultFragment getFragment(FragmentActivity activity) {
        return (OktaResultFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(AUTHENTICATION_REQUEST);
    }

    public static Intent createAuthIntent(Activity activity, Uri request,
                                          CustomTabOptions customTabOptions,
                                          String[] browsers) {
        Intent intent = new Intent(activity, OktaAuthenticationActivity.class);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_BROWSERS, browsers);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_AUTH_URI, request);
        intent.putExtra(OktaAuthenticationActivity.EXTRA_TAB_OPTIONS, customTabOptions);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SIGN_IN || requestCode == REQUEST_CODE_SIGN_OUT) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitNow();
            AuthenticationResultHandler.handler().onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
