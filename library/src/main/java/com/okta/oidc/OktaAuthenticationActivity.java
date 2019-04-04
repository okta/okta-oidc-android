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
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.okta.oidc.util.AuthorizationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

public class OktaAuthenticationActivity extends Activity {
    private static final String TAG = OktaAuthenticationActivity.class.getSimpleName();
    static final String EXTRA_AUTH_STARTED = "com.okta.auth.AUTH_STARTED";
    static final String EXTRA_AUTH_URI = "com.okta.auth.AUTH_URI";
    static final String EXTRA_TAB_OPTIONS = "com.okta.auth.TAB_OPTIONS";
    static final String EXTRA_EXCEPTION = "com.okta.auth.EXCEPTION";
    static final String EXTRA_BROWSERS = "com.okta.auth.BROWSERS";

    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String CHROME_SYSTEM = "com.google.android.apps.chrome";
    private static final String CHROME_BETA = "com.android.chrome.beta";

    @VisibleForTesting
    protected Set<String> mSupportedBrowsers = new LinkedHashSet<>();

    private CustomTabsServiceConnection mConnection;
    @VisibleForTesting
    protected boolean mAuthStarted = false;
    private Uri mAuthUri;
    @VisibleForTesting
    protected int mCustomTabColor;
    private boolean mBound = false;
    private boolean mResultSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle;
        if (savedInstanceState == null) {
            bundle = getIntent().getExtras();
        } else {
            bundle = savedInstanceState;
        }

        if (bundle != null) {
            mAuthUri = bundle.getParcelable(EXTRA_AUTH_URI);
            mCustomTabColor = bundle.getInt(EXTRA_TAB_OPTIONS, -1);
            mAuthStarted = bundle.getBoolean(EXTRA_AUTH_STARTED, false);
            if (bundle.getString(EXTRA_EXCEPTION, null) != null) {
                //login encountered exception pass same intent back to activity to handle.
                sendResult(RESULT_CANCELED, getIntent());
                return;
            }
            String[] list = bundle.getStringArray(EXTRA_BROWSERS);
            if (list != null) {
                mSupportedBrowsers.addAll(Arrays.asList(list));
            }
        }
        mSupportedBrowsers.addAll(Arrays.asList(CHROME_STABLE, CHROME_SYSTEM, CHROME_BETA));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_AUTH_STARTED, mAuthStarted);
        outState.putParcelable(EXTRA_AUTH_URI, mAuthUri);
        outState.putInt(EXTRA_TAB_OPTIONS, mCustomTabColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthStarted) {
            // The custom tab was closed without getting a result.
            sendResult(RESULT_CANCELED, null);
        } else {
            String browser = getBrowser();
            if (browser != null && !mResultSent) {
                bindServiceAndStart(browser);
            } else {
                sendResult(RESULT_CANCELED, getIntent().putExtra(EXTRA_EXCEPTION,
                        AuthorizationException.GeneralErrors.NO_BROWSER_FOUND.toJsonString()));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (OktaRedirectActivity.REDIRECT_ACTION.equals(intent.getAction())) {
            // We have successfully redirected back to this activity. Return the result and close.
            sendResult(RESULT_OK, intent);
        }
    }

    @Nullable
    @VisibleForTesting
    protected String getBrowser() {
        PackageManager pm = getPackageManager();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(browserIntent, 0);
        List<String> customTabsBrowsers = new ArrayList<>();
        for (ResolveInfo info : resolveInfoList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                customTabsBrowsers.add(info.activityInfo.packageName);
            }
        }
        for (String browser : mSupportedBrowsers) {
            if (customTabsBrowsers.contains(browser)) {
                return browser;
            }
        }
        return null;
    }

    @VisibleForTesting
    protected Intent createBrowserIntent(String packageName, CustomTabsSession session) {
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder(session);
        if (mCustomTabColor > 0) {
            intentBuilder.setToolbarColor(mCustomTabColor);
        }
        CustomTabsIntent tabsIntent = intentBuilder.build();
        tabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        tabsIntent.intent.setPackage(packageName);
        tabsIntent.intent.setData(mAuthUri);
        return tabsIntent.intent;
    }

    @Nullable
    private CustomTabsSession createSession(@NonNull CustomTabsClient client) {
        CustomTabsSession session = client.newSession(null);
        if (session == null) {
            Log.d(TAG, "Failed to create custom tabs session through custom tabs client");
            return null;
        }
        if (mAuthUri != null) {
            session.mayLaunchUrl(mAuthUri, null, Collections.emptyList());
        }
        return session;
    }

    @VisibleForTesting
    protected void bindServiceAndStart(@NonNull final String browserPackage) {
        if (mConnection != null) {
            return;
        }
        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mAuthStarted = false;
                mBound = false;
            }

            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName,
                                                     CustomTabsClient customTabsClient) {
                CustomTabsSession session = null;
                if (customTabsClient != null) {
                    customTabsClient.warmup(0);
                    session = createSession(customTabsClient);
                }
                mAuthStarted = true;
                startActivity(createBrowserIntent(browserPackage, session));
            }
        };
        mBound = CustomTabsClient.bindCustomTabsService(this, browserPackage, mConnection);
    }

    private void sendResult(int rc, Intent intent) {
        if (!mResultSent) {
            mResultSent = true;
            setResult(rc, intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null && mBound) {
            unbindService(mConnection);
            mConnection = null;
        }
        super.onDestroy();
    }
}