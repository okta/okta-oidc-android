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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.util.Log;

import com.okta.oidc.util.AuthorizationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OktaAuthenticationActivity extends Activity {
    private static final String TAG = OktaAuthenticationActivity.class.getSimpleName();
    static final String EXTRA_AUTH_STARTED = "com.okta.auth.AUTH_STARTED";
    static final String EXTRA_AUTH_URI = "com.okta.auth.AUTH_URI";
    static final String EXTRA_TAB_OPTIONS = "com.okta.auth.TAB_OPTIONS";
    static final String EXTRA_EXCEPTION = "com.okta.auth.EXCEPTION";

    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String CHROME_SYSTEM = "com.google.android.apps.chrome";
    private static final String CHROME_BETA = "com.android.chrome.beta";

    private CustomTabsServiceConnection mConnection;
    private boolean mAuthStarted = false;
    private Uri mAuthUri;
    private int mCustomTabColor;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //In case redirect activity created a new instance of auth activity.
        if (OktaRedirectActivity.REDIRECT_ACTION.equals(getIntent().getAction())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Bundle bundle;
        if (savedInstanceState == null) {
            bundle = getIntent().getExtras();
        } else {
            bundle = savedInstanceState;
        }

        if (bundle != null) {
            if (bundle.getString(EXTRA_EXCEPTION, null) != null) {
                //login encountered exception pass same intent back to activity to handle.
                sendResult(RESULT_CANCELED, getIntent());
                finish();
                return;
            }
            mAuthUri = bundle.getParcelable(EXTRA_AUTH_URI);
            mCustomTabColor = bundle.getInt(EXTRA_TAB_OPTIONS, -1);
            mAuthStarted = bundle.getBoolean(EXTRA_AUTH_STARTED, false);
            String browser = getBrowser();
            if (browser != null) {
                bindServiceAndStart(browser);
            } else {
                setResult(RESULT_CANCELED, getIntent().putExtra(EXTRA_EXCEPTION,
                        AuthorizationException.GeneralErrors.NO_BROWSER_FOUND));
                finish();
            }
        }
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
        if (!mAuthStarted) {
            // The custom tab was closed without getting a result.
            sendResult(RESULT_CANCELED, null);
        }
        mAuthStarted = false;
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
    private String getBrowser() {
        PackageManager pm = getPackageManager();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
        int queryFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PackageManager.MATCH_ALL
                : PackageManager.MATCH_DEFAULT_ONLY;
        ResolveInfo resolveInfo = pm.resolveActivity(browserIntent, queryFlag);
        String browser = null;
        if (resolveInfo != null) {
            browser = resolveInfo.activityInfo.packageName;
        }

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

        if (customTabsBrowsers.contains(browser)) {
            return browser;
        } else if (customTabsBrowsers.contains(CHROME_STABLE)) {
            return CHROME_STABLE;
        } else if (customTabsBrowsers.contains(CHROME_SYSTEM)) {
            return CHROME_SYSTEM;
        } else if (customTabsBrowsers.contains(CHROME_BETA)) {
            return CHROME_BETA;
        } else if (!customTabsBrowsers.isEmpty()) {
            return customTabsBrowsers.get(0);
        } else {
            return null;
        }
    }

    private Intent createBrowserIntent(String packageName, CustomTabsSession session) {
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

    private void bindServiceAndStart(@NonNull final String browserPackage) {
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
                customTabsClient.warmup(0);
                CustomTabsSession session = createSession(customTabsClient);
                if (session != null) {
                    startActivity(createBrowserIntent(browserPackage, session));
                } else {
                    setResult(RESULT_CANCELED, getIntent().putExtra(EXTRA_EXCEPTION,
                            AuthorizationException.GeneralErrors.NO_BROWSER_FOUND));
                }
            }
        };
        mAuthStarted = true;
        mBound = CustomTabsClient.bindCustomTabsService(this, browserPackage, mConnection);
    }

    private void sendResult(int rc, Intent intent) {
        setResult(rc, intent);
        finish();
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