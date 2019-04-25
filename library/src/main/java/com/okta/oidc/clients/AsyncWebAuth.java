package com.okta.oidc.clients;

import com.okta.oidc.AuthenticationPayload;
import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.clients.sessions.AsyncSession;
import com.okta.oidc.util.AuthorizationException;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

/**
 * Client Web Async API for Okta OpenID Connect & OAuth 2.0 APIs.
 * <pre>
 * {@code
 * OIDCConfig account = new OIDCConfig.Builder()
 *     .clientId("{clientId}")
 *     .redirectUri("{redirectUri}")
 *     .endSessionRedirectUri("{endSessionUri}")
 *     .scopes("openid", "profile", "offline_access")
 *     .discoveryUri("https://{yourOktaDomain}")
 *     .create();
 *
 * AsyncWebAuth client = new Okta.AsyncWebBuilder()
 *     .withConfig(config)
 *     .withContext(getApplicationContext())
 *     .withStorage(new SimpleOktaStorage(this))
 *     .withTabColor(getColorCompat(R.color.colorPrimary))
 *     .withCallbackExecutor(Executors.newSingleThreadExecutor())
 *     .supportedBrowsers(new String[]{CHROME_PACKAGE_ID, FIREFOX_PACKAGE_ID})
 *     .create();
 * }
 * </pre>
 * <p>
 * Note that callbacks are executed on the UI thread unless a executor is provided to the builder.
 *
 * @see <a href="https://developer.okta.com/docs/api/resources/oidc/">Okta API docs</a>
 */
public interface AsyncWebAuth extends BaseAuth<AsyncSession> {
    boolean isInProgress();

    /**
     * Log in using implicit flow.
     * <p>
     * The result will be returned in the {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param activity the activity
     * @param payload  the {@link AuthenticationPayload payload}
     */
    void logIn(@NonNull final FragmentActivity activity, AuthenticationPayload payload);

    /**
     * Sign out from okta. This will clear the browser session
     * <p>
     * The result will be returned in the {@link #registerCallback(ResultCallback, FragmentActivity)} callback
     *
     * @param activity the activity
     */
    void signOutFromOkta(@NonNull final FragmentActivity activity);

    /**
     * Register a callback for login and logout result status. The callback is triggered when
     * {@link #logIn(FragmentActivity, AuthenticationPayload) logIn} or
     * {@link #signOutFromOkta(FragmentActivity)} signOutFromOkta} is completed.
     * Example usage:
     * <p>
     * {@code
     * <pre>
     * client.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
     *      @Override
     *      public void onSuccess(@NonNull AuthorizationStatus status) {
     *          if (status == AuthorizationStatus.AUTHORIZED) {
     *              //login success. client can be used to perform protected resource requests.
     *          } else if (status == AuthorizationStatus.LOGGED_OUT) {
     *              //logout success. browser session is cleared.
     *          } else if (status == AuthorizationStatus.IN_PROGRESS) {
     *              //request in progress.
     *          }
     *      }
     *
     *      @Override
     *      public void onCancel() {
     *          //request canceled
     *      }
     *
     *      @Override
     *      public void onError(@NonNull String msg, AuthorizationException error) {
     *          //error
     *      }
     * }, this);
     * </pre>
     * }
     *
     * @param resultCallback returns the result of login or logout attempts.
     * @param activity       the activity which will receive the results.
     */
    void registerCallback(ResultCallback<AuthorizationStatus, AuthorizationException> resultCallback, FragmentActivity activity);
    void unregisterCallback();
}
