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
import android.provider.Browser;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import com.okta.oidc.util.AuthorizationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.okta.oidc.net.ConnectionParameters.USER_AGENT_HEADER;
import static com.okta.oidc.net.ConnectionParameters.X_OKTA_USER_AGENT;

/**
 * This activity starts the authorization with PKCE flow.
 *
 * @see "Authorization Code with PKCE flow <https://developer.okta.com/authentication-guide/auth-overview/#authorization-code-with-pkce-flow>"
 * @see "Implementing the Authorization Code with PKCE flow <https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce/>"
 */
public class OktaAuthenticationActivity extends Activity {
    private static final String TAG = OktaAuthenticationActivity.class.getSimpleName();
    /**
     * The Extra auth started.
     */
    static final String EXTRA_AUTH_STARTED = "com.okta.auth.AUTH_STARTED";
    /**
     * The Extra auth uri.
     */
    static final String EXTRA_AUTH_URI = "com.okta.auth.AUTH_URI";
    /**
     * The Extra tab options.
     */
    static final String EXTRA_TAB_OPTIONS = "com.okta.auth.TAB_OPTIONS";
    /**
     * The Extra exception.
     */
    static final String EXTRA_EXCEPTION = "com.okta.auth.EXCEPTION";
    /**
     * The Extra browsers.
     */
    static final String EXTRA_BROWSERS = "com.okta.auth.BROWSERS";

    private static final String CHROME_STABLE = "com.android.chrome";
    private static final String CHROME_SYSTEM = "com.google.android.apps.chrome";
    private static final String CHROME_BETA = "com.android.chrome.beta";

    /**
     * The M supported browsers.
     */
    @VisibleForTesting
    protected Set<String> mSupportedBrowsers = new LinkedHashSet<>();

    private CustomTabsServiceConnection mConnection;
    /**
     * The M auth started.
     */
    @VisibleForTesting
    protected boolean mAuthStarted = false;
    private Uri mAuthUri;

    /**
     * The custom tab options.
     */
    @VisibleForTesting
    protected CustomTabOptions mCustomTabOptions;
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
            mCustomTabOptions = bundle.getParcelable(EXTRA_TAB_OPTIONS);
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
        outState.putParcelable(EXTRA_TAB_OPTIONS, mCustomTabOptions);
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
                sendResult(RESULT_OK, getIntent().putExtra(EXTRA_EXCEPTION,
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

    /**
     * Gets the chrome custom tab web browser package.
     *
     * @return the browser package name.
     */
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
        //Use first compatible browser on list.
        if (!customTabsBrowsers.isEmpty()) {
            return customTabsBrowsers.get(0);
        }
        return null;
    }

    /**
     * Create browser intent intent.
     *
     * @param packageName the package name
     * @param session     the session
     * @return the intent
     */
    @VisibleForTesting
    protected Intent createBrowserIntent(String packageName, CustomTabsSession session) {
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder(session);
        if (mCustomTabOptions != null) {
            if (mCustomTabOptions.getCustomTabColor() != 0) {
                intentBuilder.setToolbarColor(mCustomTabOptions.getCustomTabColor());
            }
            if (mCustomTabOptions.getStartExitResId() != 0 &&
                    mCustomTabOptions.getStartEnterResId() != 0) {
                intentBuilder.setStartAnimations(this,
                        mCustomTabOptions.getStartEnterResId(),
                        mCustomTabOptions.getStartExitResId());
                overridePendingTransition(mCustomTabOptions.getStartEnterResId(),
                        mCustomTabOptions.getStartExitResId());
            }
            if (mCustomTabOptions.getEndEnterResId() != 0 &&
                    mCustomTabOptions.getEndExitResId() != 0) {
                intentBuilder.setExitAnimations(this, mCustomTabOptions.getEndEnterResId(),
                        mCustomTabOptions.getEndExitResId());
            }
        }
        CustomTabsIntent tabsIntent = intentBuilder.build();
        tabsIntent.intent.setPackage(packageName);
        tabsIntent.intent.setData(mAuthUri);
        Bundle headers = new Bundle();
        headers.putString(X_OKTA_USER_AGENT, USER_AGENT_HEADER);
        tabsIntent.intent.putExtra(Browser.EXTRA_HEADERS, headers);
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

    /**
     * Bind service and start.
     *
     * @param browserPackage the browser package
     */
    @VisibleForTesting
    protected void bindServiceAndStart(@NonNull final String browserPackage) {
        if (mConnection != null) {
            return;
        }
        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mAuthStarted = false;
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
        CustomTabsClient.bindCustomTabsService(this, browserPackage, mConnection);
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
        if (mConnection != null) {
            unbindService(mConnection);
            mConnection = null;
        }
        super.onDestroy();
    }
}
