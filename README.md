[<img src="https://aws1.discourse-cdn.com/standard14/uploads/oktadev/original/1X/0c6402653dfb70edc661d4976a43a46f33e5e919.png" align="right" width="256px"/>](https://devforum.okta.com/)
[![CI Status](http://img.shields.io/travis/okta/okta-oidc-android.svg?style=flat)](https://travis-ci.org/okta/okta-oidc-android)

# Okta OpenID Connect & OAuth 2.0 Library

## Table of Contents

- [Overview](#Overview)
  - [Requirements](#Requirements)
  - [Installation](#Installation)
  - [Sample app](#Sample-app)
- [Add a URI Scheme](#Add-a-URI-Scheme)  
- [Configuration](#Configuration)
  - [Using JSON configuration file](#Using-JSON-configuration-file)
- [Sign in with a browser](#Sign-in-with-a-browser)
  - [onActivityResult override](#onActivityResult-override)
  - [Social login](#Social-login)
- [Sign in with your own UI](#Sign-in-with-your-own-UI)
- [Sign out](#Sign-out)
  - [Clear browser session](#Clear-browser-session)
  - [Revoke tokens (optional)](#Revoke-tokens-(optional))
  - [Clear tokens from device](#Clear-tokens-from-device)
- [Using the Tokens](#Using-the-Tokens)
  - [Get user information](#Get-user-information)
  - [Performing authorized requests](#Performing-authorized-requests)
  - [Refresh a Token](#Refresh-a-Token)
  - [Revoking a Token](#Revoking-a-Token)
  - [Introspect a token](#Introspect-a-token)
- [Token management](#Token-management)
- [Advance configuration](#Advance-configuration)
  - [Client variants](#Client-variants)
  - [Providing browser used for authorization](#Providing-browser-used-for-authorization)
  - [Customize HTTP requests](#Customize-HTTP-requests)
  - [Storage](#Storage)
  - [Encryption](#Encryption)
  - [Hardware-backed keystore](#Hardware-backed-keystore)
  - [Encryption and decryption errors](#Encryption-and-decryption-errors)
- [Advanced techniques](#Advanced-techniques)
  - [Sign in with a sessionToken (Async)](#Sign-in-with-a-sessionToken-(Async))
  - [Sign in with a sessionToken (Sync)](#Sign-in-with-a-sessionToken-(Sync))
  - [Multiple Authorization Clients](#Multiple-authorization-clients)

## Overview

This library is for communicating with Okta as an OAuth 2.0 + OpenID Connect provider, and follows current best practice for native apps using [Authorization Code Flow + PKCE](https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce).

You can learn more on the [Okta + Android](https://developer.okta.com/code/android/) page in our documentation. For more information about [Okta OpenID Connect & OAuth 2.0 API](https://developer.okta.com/docs/api/resources/oidc/).

### Requirements

Okta OIDC SDK supports Android API 21 and above. [Chrome custom tab][chrome-custom-tabs] enabled browsers
are needed by the library for browser initiated authorization. An Okta developer account is needed to run the sample.
It is recommended that your app extends [FragmentActivity][fragment-activity] or any extensions of it. If you are extending [Activity][activity], you have to override [onActivityResult](#onActivityResult-override).

### Installation

**Note:** If you're updating from 1.0.18 or earlier, we changed the maven coordinates, and where it's hosted. See the [CHANGELOG](/CHANGELOG.md/#1019) for more information.

Add the `Okta OIDC` dependency to your `build.gradle` file:

```gradle
implementation 'com.okta.android:okta-oidc-android:1.2.1'
```

The SDK requires Java 8 support.
To enable, add the following following to your `build.gradle` file

```gradle
android {
    ...
    ...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    // For Kotlin projects
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

For more details on Java 8 support for your Android projects, refer to the [Android developer documentations](#https://developer.android.com/studio/write/java8-support)

### Sample app

A sample is contained within this repository. For more information on how to
build, test and configure the sample, see the sample [README](https://github.com/okta/okta-oidc-android/blob/master/app/README.md).

## Add a URI Scheme

Similar to the sample app, you must add a redirect scheme to receive sign in results from the web browser. To do this, you must define a gradle manifest placeholder in your app's build.gradle:

```gradle
android.defaultConfig.manifestPlaceholders = [
    "appAuthRedirectScheme": "com.okta.oidc.example"
]
```

**Note** The SDK doesn't allow multiple apps to use the same scheme. If it detects more than one application sharing the same scheme it will throw an exception.

## Configuration

First the authentication client must have a config to interact with Okta's OIDC provider. Create a `OIDCConfig` like the following example:

```java
config = new OIDCConfig.Builder()
    .clientId("{clientId}")
    .redirectUri("{redirectUri}")
    .endSessionRedirectUri("{endSessionUri}")
    .scopes("openid", "profile", "offline_access")
    .discoveryUri("https://{yourOktaDomain}")
    .create();
```

Then create a `client` like the following:

```Java
WebAuthClient webClient = new Okta.WebAuthBuilder()
                .withConfig(config)
                .withContext(getApplicationContext())
                .withStorage(new SharedPreferenceStorage(this))
                .create();
```

After creating the client, register a callback to receive authorization results.

```java
SessionClient sessionClient = webClient.getSessionClient();
webClient.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull AuthorizationStatus status) {
        if (status == AuthorizationStatus.AUTHORIZED) {
            //client is authorized.
            Tokens tokens = sessionClient.getTokens();
        } else if (status == AuthorizationStatus.SIGNED_OUT) {
            //this only clears the browser session.
        }
    }

    @Override
    public void onCancel() {
        //authorization canceled
    }

    @Override
    public void onError(@NonNull String msg, AuthorizationException error) {
     //error encounted
    }
}, this);
```

The `client` can now be used to authenticate users and authorize access.

### Discovery URI Guidance

Be aware that other apps will reject tokens that do not have `aud` claim set to audience value configured in authorization server. If you require your token to be validated in other apps - make sure you have created custom authorization server for your apps at https://yourOktaDomain-admin.okta.com/admin/oauth2/as specifying the audience for which token is intended for and created respective `OIDCConfig` with proper issuer/discoveryUri, do not use auth servers with audience set to Okta org itself (as it can only be used with Okta org in the userinfo request to get user claims). For more information about Okta Authorization Servers please see https://developer.okta.com/docs/concepts/auth-servers/

**Note**: `.well-known/openid-configuration` will be appended to your `discoveryUri` if it is missing.

- `discoveryUri` is: `https://{yourOktaDomain}/oauth2/${authServerId}` then `.well-known/openid-configuration` is added.
- `discoveryUri` is: `https://{yourOktaDomain}` then `.well-known/openid-configuration` is added.
- `discoveryUri` is: `https://{yourOktaDomain}/oauth2/default` then `.well-known/openid-configuration` is added.
- `discoveryUri` is: `https://{yourOktaDomain}/oauth2/${authServerId}/.well-known/openid-configuration` nothing is added.
- `discoveryUri` is: `https://{yourOktaDomain}/oauth2/${authServerId}/.well-known/oauth-authorization-server` nothing is added.

For more information about the metadata returned by the different server configurations:

[OpenID Connect (.well-known/openid-configuration)](https://developer.okta.com/docs/reference/api/oidc/#well-known-openid-configuration)

[OAuth 2.0 (.well-known/oauth-authorization-server)](https://developer.okta.com/docs/reference/api/oidc/#well-known-oauth-authorization-server)

### Using JSON configuration file

You can also create a `config` by providing a JSON file.
Create a file called `okta_oidc_config.json` in your application's `res/raw/` directory with the following contents:

```json
{
  "client_id": "{clientId}",
  "redirect_uri": "{redirectUri}",
  "end_session_redirect_uri": "{endSessionUri}",
  "scopes": [
    "openid",
    "profile",
    "offline_access"
  ],
  "discovery_uri": "https://{yourOktaDomain}"
}
```

Use this JSON file to create a `configuration`:

```java
OIDCConfig config = new OIDCConfig.Builder()
    .withJsonFile(this, R.raw.okta_oidc_config)
    .create();
```
## Configuration without discoveryUri

It is recommended to use `discoveryUri` to retrieve your authorization providers configuration.
But if your provider does not have a `discoveryUri` you can provide the URIs to the endpoints
yourself like the following:

```java
CustomConfiguration config = new CustomConfiguration.Builder()
    .tokenEndpoint("{yourTokenEndpoint}")
    .authorizationEndpoint("{yourAuthorizeEndpoint}")
    .create();
        
mOidcConfig = new OIDCConfig.Builder()
    .clientId(BuildConfig.CLIENT_ID)
    .redirectUri(BuildConfig.REDIRECT_URI)
    .endSessionRedirectUri(BuildConfig.END_SESSION_URI)
    .scopes(BuildConfig.SCOPES)
    .customConfiguration(config)
    .create();
```

**Note**: To receive a [refresh_token](https://developer.okta.com/docs/guides/refresh-tokens/overview/), you must include the `offline_access` scope.

## Sign in with a browser

The authorization flow consists of four stages.

1. Service discovery - This uses the discovery uri to get a list of endpoints.
2. Authorizing the user with [chrome custom tabs][chrome-custom-tabs] to obtain an authorization code.
3. Exchanging the authorizaton code for a access token, ID token, and/or refresh token.
4. Using the tokens to interact with a resource server to access user data.

This is all done in the background by the SDK. For example to sign in you can call:

```java
client.signIn(this, null);
```

The results will be returned in the registered callback. If the application needs to send extra
data to the api endpoint, `AuthenticationPayload` can be used:

```java
AuthenticationPayload payload = new AuthenticationPayload.Builder()
    .setLoginHint("youraccount@okta.com")
    .addParameter("max_age", "5000")
    .build();

client.signIn(this, payload);
```

### onActivityResult override

The library uses a nested fragment to abstract the redirect callback. It uses [onActivityResult][on-activity-result] to receive data from the browser. If your app overrides [onActivityResult][on-activity-result] you must call
`super.onActivityResult()` to propagate unhandled `requestCode` to the library's fragment.

```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
```

If your app extends [Activity][activity] instead of [FragmentActivity][fragment-activity] or [AppCompatActivity](https://developer.android.com/reference/android/support/v7/app/AppCompatActivity) you must override [onActivityResult][on-activity-result] and pass the result to `WebAuthClient`.

```java
public class PlainActivity extends Activity {
    private WebAuthClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OIDCConfig config = new OIDCConfig.Builder()
                        .withJsonFile(this, R.raw.okta_oidc_config)
                        .create();

        client = new Okta.WebAuthBuilder()
                        .withConfig(mOidcConfig)
                        .withContext(this)
                        .create();

        ResultCallback<AuthorizationStatus, AuthorizationException> callback =
            new ResultCallback<AuthorizationStatus, AuthorizationException>() {
                            @Override
                            public void onSuccess(@NonNull AuthorizationStatus status) {
                            }

                            @Override
                            public void onCancel() {
                            }

                            @Override
                            public void onError(@Nullable String msg, AuthorizationException error) {
                            }
                        };
        client.registerCallback(callback, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //must pass the results back to the WebAuthClient.
        client.handleActivityResult(requestCode, resultCode, data);
    }
}
```

### Social login

To use another identity provider such as [Google][IDP-Google] or [Facebook][IDP-Facebook], first [step up the identity provider in Okta][IDP]. Once your setup is complete you can sign in using the social login provider.

```java
AuthenticationPayload payload = new AuthenticationPayload.Builder()
    .setIdp("appID_or_clientID_of_your_idp")
    .setIdpScope("scope_of_your_idp")
    .build();

client.signIn(this, payload);
```

Sign in will be redirected to the page of the specified IDP.

## Sign in with your own UI

If you would like to use your own in-app user interface instead
of the web browser, you can do so by using a [sessionToken][session-token]:

```java
AuthClient authClient = new Okta.AuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SharedPreferenceStorage(this))
    .create();
```

After building the `AuthClient` you should call `signIn` method where you need to provide a [sessionToken][session-token] and `RequestCallback`

```java
SessionClient sessionClient = authClient.getSessionClient();
if (!sessionClient.isAuthenticated()) {
    authClient.signIn("{sessionToken}", null, new RequestCallback<Result, AuthorizationException>() {
        @Override
        public void onSuccess(@NonNull Result result) {
            //client is now authorized.
        }

        @Override
        public void onError(String error, AuthorizationException exception) {
            //handle error
        }
    });
}

```

### Okta Java Authentication Setup

To get a **sessionToken**, you must use [Okta's Authentication API](https://developer.okta.com/docs/api/resources/authn). You can use [Okta Java Authentication SDK](https://github.com/okta/okta-auth-java) to get a [sessionToken][session-token]. An example of using the Authentication API can be found [here](https://github.com/okta/samples-android/tree/master/sign-in-kotlin). The Authentication SDK is only available for API 24 and above. If using API < 24, you must use Android Studio 4.0 or higher with [Java 8+ API desugaring][java-8-api].

To enable Java 8+ API support, add the following to your gradle file:

```gradle
android {
    ...
    ...
    compileOptions {
        coreLibraryDesugaringEnabled true
        sourceCompatibility 1.8
        targetCompatibility 1.8
    }
    // If using kotlin
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies {
    ...
    ...
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.0.10'
    implementation 'com.okta.android:okta-oidc-android:1.2.1'
    implementation 'com.okta.authn.sdk:okta-authn-sdk-api:2.0.0'
    implementation('com.okta.authn.sdk:okta-authn-sdk-impl:2.0.0') {
        exclude group: 'com.okta.sdk', module: 'okta-sdk-httpclient'
    }
}
```

## Sign out

If the user is signed in using the browser initiated authorization flow, then signing out
is a two or three step process depending on revoking the tokens.

1. Clear the browser session.
2. [Revoke the tokens](#Revoking-a-Token) (optional)
3. Clear the app session (stored tokens) in [memory](#Token-management).

If the user is signed in using a [sessionToken](#Sign-in-with-your-own-UI) you can skip clearing the browser.

### Clear browser session

In order to clear the browser session you have to call `signOutOfOkta()`.

```java
    client.signOutOfOkta(this);
```

This clears the current browser session only. It does not remove or revoke the cached tokens stored in the `client`.
Until the tokens are removed or revoked, the user can still access data from the resource server.

### Revoke tokens (optional)

Tokens are still active (unless expired) even if you have cleared the browser session. An optional step is to revoke the tokens to make them inactive. Please see [Revoke the tokens](#Revoking-a-Token).

### Clear tokens from device

Tokens can be removed from the device by simply calling:

```java
    client.getSessionClient().clear();
```

After this the user is signed out.

#### Sign out wrapper

You can also call `signOut()` which wraps all these steps in one call.

```java
 authClient.signOut(this, new RequestCallback<Integer, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull Integer result) {
        if (result == SUCCESS) {
            //signed out
        }
        if ((result & FAILED_CLEAR_SESSION) == FAILED_CLEAR_SESSION) {
            //session not cleared
        }
        if ((result & FAILED_REVOKE_ACCESS_TOKEN) == FAILED_REVOKE_ACCESS_TOKEN) {
            //access token revocation failed.
        }
        if ((result & FAILED_REVOKE_REFRESH_TOKEN) == FAILED_REVOKE_REFRESH_TOKEN) {
            //refresh token revocation failed.
        }
        if ((result & FAILED_CLEAR_DATA) == FAILED_CLEAR_DATA) {
            //failed to remove data.
        }
    }

    @Override
    public void onError(@Nullable String msg, @Nullable AuthorizationException exception) {
        //NO-OP
    }
});
```

If any step fails, it will still process to the next step. It is recommended to do these steps individually to give your application more control of the sign out process.

**Note** `signOut()` does not save the application state so if the activity is destroyed during these steps you should call it again to start the sign out process over.

## Using the Tokens

Once the user is authorized you can use the client object to call the OIDC endpoints, in order to access this API you need to get a `sessionClient`

```java
sessionClient = client.getSessionClient();
```

### Get user information

An example of getting user information from [userinfo](https://developer.okta.com/docs/api/resources/oidc/#userinfo) endpoint:

```java
sessionClient.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull UserInfo result) {
        //handle UserInfo result.
    }

    @Override
    public void onError(String error, AuthorizationException exception) {
        //handle failed userinfo request
    }
});
```

In `onSuccess` the userinfo returned is a `UserInfo` with the [response properties](https://developer.okta.com/docs/api/resources/oidc/#response-example-success-5).

### Performing authorized requests

Authorized request to your own server endpoints will need to add the `Authorization` header with the `access token`, prefixed by the standard OAuth 2.0 of `Bearer`.
If you are using Android's standard `HttpURLConnection` you can set the headers like the following:

```java
try {
    Tokens token = client.getSessionClient.getTokens();
    URL url = new URL("yourCustomUrl");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestProperty("Authorization", "Bearer " + token.getAccessToken());
} catch (AuthorizationException e) {
    //handle error
}
```

If you are using `OkHttp` you can set the headers in the `Request` like the following:

```java
try {
    Tokens token = client.getSessionClient.getTokens();
    Request request = new Request.Builder()
        .url("yourCustomUrl")
        .addHeader("Authorization", "Bearer " + token.getAccessToken())
        .build();
} catch (AuthorizationException e) {
    //handle error
}
```

### Refresh a Token

You can refresh the `tokens` with the following request:

```java
client.getSessionClient().refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull Tokens result) {
        //handle success.
    }

    @Override
    public void onError(String error, AuthorizationException exception) {
        //handle request failure
    }
});
```

### Revoking a Token

Tokens can be revoked with the following request:

```java
try {
    Tokens token = client.getSessionClient.getTokens();
    client.getSessionClient().revokeToken(token.getRefreshToken(),
        new RequestCallback<Boolean, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull Boolean result) {
                //handle result
            }
            @Override
            public void onError(String error, AuthorizationException exception) {
                //handle request error
            }
        });
} catch (AuthorizationException e) {
    //handle error
}
```

**Note:** *Access, refresh and ID tokens need to be revoked in separate requests. The request only revokes the specified token*

### Introspect a token

Tokens can be checked for more detailed information by using the introspect endpoint:

```java
try {
    client.getSessionClient().introspectToken(client.getTokens().getRefreshToken(),
        TokenTypeHint.REFRESH_TOKEN, new RequestCallback<IntrospectInfo, AuthorizationException>() {
            @Override
            public void onSuccess(@NonNull IntrospectInfo result) {
                //handle introspect response.
            }

            @Override
            public void onError(String error, AuthorizationException exception) {
                //handle request error
            }
        }
    );
} catch (AuthorizationException e) {
    //handle error
}
```

A list of the response properties can be found [here](https://developer.okta.com/docs/api/resources/oidc/#response-properties-3)

## Token management

Tokens are encrypted and securely stored in the private Shared Preferences.
If you do not want `AuthenticateClient` to store the data you can pass in a empty interface when creating the `client`

```java

client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new OktaStorage() {
                @Override
                public void save(@NonNull String key, @NonNull String value) {
                }
                @Override
                public String get(@NonNull String key) {
                    return null;
                }
                @Override
                public void delete(@NonNull String key) {
                }})
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .create();
```

The library provides a storage interface and encryption interface. These interfaces allow the developer to override the default implementation if they wish to use custom encryption or storage mechanism. For more see the [advance configuration](#Providing-custom-storage) section.

## Advance configuration

The library allows customization to specific parts the SDK to meet developer needs.

### Client variants

The library provides asynchronous and synchronous variant of each client type. The corresponding `SessionClient` created from the `AuthClient` will have the same asynchronous or synchronous behavior. The following shows how to create different type of clients.

### WebAuthClient

`WebAuthClient` redirects to a [chrome custom tabs][chrome-custom-tabs] enabled browser for authenticaiton.
The following shows how to create a asynchronous web authentication client.

```java
WebAuthClient webAuthClient = new Okta.WebAuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SharedPreferenceStorage(this))
        .withCallbackExecutor(Executors.newSingleThreadExecutor())
        .withTabColor(Color.BLUE)
        .supportedBrowsers("com.android.chrome", "org.mozilla.firefox")
        .create();
```

### SyncWebAuthClient

The following shows how to create synchronous web authentication client:

```java
SyncWebAuthClient webSyncAuthClient = new Okta.SyncWebAuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SharedPreferenceStorage(this))
        .withTabColor(Color.BLUE)
        .supportedBrowsers("com.android.chrome", "com.google.android.apps.chrome", "com.android.chrome.beta")
        .create();
```

### AuthClient

`AuthClient` will require a [sessionToken][session-token]. See [Sign in with your own UI](#Sign-in-with-your-own-UI) for more information on how to obtain a [sessionToken][session-token].
The following shows how to create a asynchronous authentication client:

```java
AuthClient authClient = new Okta.AuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SharedPreferenceStorage(this))
        .withCallbackExecutor(Executors.newSingleThreadExecutor())
        .create();
```

### SyncAuthClient

The following shows how to create synchronous authentication client:

```java
SyncAuthClient syncAuthClient = new Okta.SyncAuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SharedPreferenceStorage(this))
        .create();
```

### Providing browser used for authorization

The default browser used for authorization is Chrome. If you want to change it FireFox, you can add this in the various `Okta.WebAuthBuilder()`:

```java

String SAMSUNG = "com.sec.android.app.sbrowser";
String FIREFOX = "org.mozilla.firefox";

//ANDROID_BROWSER DOES NOT SUPPORT CHROME CUSTOM TABS! Won't work.
String ANDROID_BROWSER = "com.android.browser";

client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SharedPreferenceStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

The library will attempt to use FireFox then Samsung browsers first.
If none are found it will default to Chrome.

**Note**: The library only supports [Chrome custom tab](https://developer.chrome.com/multidevice/android/customtabs) enabled browsers. If no compatible browsers are found you'll receive a `AuthorizationException` with a `No compatible browser found` message. You should handle this error by redirecting the user to download a compatible browser in the app store.

### Customize HTTP requests

You can customize how HTTP connections are made by implementing the `OktaHttpClient` interface:

```java
private class MyHttpClient implements OktaHttpClient {
    //Implement interface
    //...
}

client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SharedPreferenceStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .withOktaHttpClient(new MyHttpClient())
    .create();
```

For an example on using [OkHttp](https://github.com/okta/okta-oidc-android/blob/master/app/src/main/java/com/okta/oidc/example/OkHttp.java).

### Storage

The library provides storage using shared preferences. If you wish to use SQL or any other storage mechanism you can implement the storage interface and use it when creating the various `AuthClient`.
The default behavior requires a hardware-backed keystore for encryption. If the device does not provide hardware-backed keystore the library will not store any data. If you wish to override this behavior you can set this option the `Builder`:

```java

client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new MyStorage())
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .setRequireHardwareBackedKeyStore(false)
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

### Encryption

Encryption is applied to all data that is stored by the library. You can specify your own encryption algorithm with the following steps:

Build your own implementation of `EncryptionManager`

```java
public class CustomEncryptionManager implements EncryptionManager {
    @Override
    public String encrypt(String data) throws GeneralSecurityException {
        //encryt data
        return encryptedData;
    }

    @Override
    public String decrypt(String encryptedData) throws GeneralSecurityException {
        if (value != null && value.length() > 0) {
            //decrypt data
            return decryptedData;
        } else {
            return null;
        }
    }
    //...
    //...
}
```

Provide it within selected Okta Client Builder

```java
client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new MyStorage())
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .withEncryptionManager(new CustomEncryptionManager())
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

This will allow the SDK to use your `CustomEncryptionManager`, if you want to disable encryption you can simply return the parameter that was passed into encrypt and decrypt.
The SDK provides two implementations of `EncryptionManager`

## DefaultEncryptionManager

Private keys are stored in KeyStore. Does not require device authentication to use the keys. Compatible with API19 and up. This is a RSA implementation that

## GuardedEncryptionManager

Private keys are stored in KeyStore. Requires device authentication to use the keys. Compatible with API23 and up.

### Hardware-backed keystore

The default `EncryptionManager` provides a check to see if the device supports hardware-backed keystore. If you implement your own `EncryptionManager` you'll have to implement this check. You can return `true` to tell the default storage that the device have a hardware-backed keystore. The [storage](#Storage) and [encrytion](#Encryption) mechanisms work together to ensure that data is stored securely.

### Encryption and decryption errors

Most of the encrytion errors encountered are due to the key being invalidated. This means that the initial key that was used to encrypt the data have become inaccessible. Once this happens it is impossible to recover the encrypted data. To recover from this you must clear the data:

```java
@Override
public void onError(@Nullable String msg, AuthorizationException error) {
    if (error.type == ILLEGAL_BLOCK_SIZE &&
        error.code == EncryptionErrors.ILLEGAL_BLOCK_SIZE) {
        sessionClient.clear();
    }
}
```

#### Why am I getting invalid key errors

A invalid key error usually means that the initial key that was used to encrypt the data have become inaccessible. The following is a list of possible causes:

1. Keys are invalidated by security policies on the device. For example some devices invalidate keys when switching from PIN to Pattern lock. The policy differs by device and OS version.

2. Keys are inaccessible when application is uninstall but data is backed up. For example when the application is uninstalled but the shared preferences remain. When attempting to sign-in the SDK will attempt to decrypt this data and fail, to resolve the error simply clear the data and try again. This usually happens during development when uninstalling and reinstalling is common.

3. Keys are lost during application updates. Sometimes keys are inaccessible when updating the application. When this happens it is best to recover by clearing the data. This will require users to sign-in to the appication again.

4. Encryption bug with the underlying OS. The SDK uses a workaround for a known [RSA issue](https://issuetracker.google.com/issues/37075898). If the workaround is not working on your device it is best to implement a [custom encryption manager](#Encryption) and handle the encrytion by using a encryption algorithm that the device supports. For example if using [androidx.security.crypto library](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) as the custom encrytion manager, we need to [disable encryption](https://github.com/okta/okta-oidc-android/blob/master/app/src/main/java/com/okta/oidc/example/NoEncryption.java) then implement the [encrypted storage](https://github.com/okta/okta-oidc-android/blob/master/app/src/main/java/com/okta/oidc/example/EncryptedSharedPreferenceStorage.java).

```java
EncryptedSharedPreferenceStorage storage = null;
try {
    storage = new EncryptedSharedPreferenceStorage(this);
} catch (GeneralSecurityException | IOException ex) {
    Log.d(TAG, "Unable to initialize EncryptedSharedPreferenceStorage", ex);
}

authClient = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withEncryptionManager(new NoEncryption())
    .withStorage(storage)
    .create();
```

## Advanced techniques

Sometimes as a developer you want to have more control over SDK and here is a couple of advanced API's that are available to give
you more control as a developer.

### Sign in with a sessionToken (Async)

In order to use authentication flow without browser you can use our `AuthClient`

```java
AuthClient authClient = new Okta.AuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SharedPreferenceStorage(this))
    .create();
```

After building `AuthClient` you should call `signIn` method where you need provide [sessionToken][session-token] and `RequestCallback`

```java
authClient.signIn("{sessionToken}", null, new RequestCallback<Result, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull Result result) {

    }

    @Override
    public void onError(String error, AuthorizationException exception) {

    }
});
```

Optionally you can provide `AuthenticationPayload` as a part of sign in call.

### Sign in with a sessionToken (Sync)

In order to use a synchronous authentication flow without a browser you can use our `SyncAuthClient`

```java
SyncAuthClient syncAuthClient = new Okta.SyncAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SharedPreferenceStorage(this))
    .create();
```

After building `SyncAuthClient` you should call `signIn` method where you need provide [sessionToken][session-token]
NOTE: that is a synchronous call so please check that it is not performed on Ui Thread.

```java
syncAuthClient.signIn("sessionToken", null)
```

Optionally you can provide `AuthenticationPayload` as a part of sign in call.

### Multiple authorization clients

Multiple `AuthClient` are supported. However for `WebAuthClient` only one callback can be registered. For example you can have multiple authorization servers redirecting to the same application:

```java
OIDCConfig configFirstApp = new OIDCConfig.Builder()
    .withJsonFile(this, R.raw.okta_oidc_config_first)
    .create();

//config file with different domain, client_id than config_first but same redirect_uri
OIDCConfig configSecondApp = new OIDCConfig.Builder()
    .withJsonFile(this, R.raw.okta_oidc_config_second)
    .create();

WebAuthClient webAuthFirstApp = new Okta.WebAuthBuilder()
                .withConfig(configFirstApp)
                .withContext(getApplicationContext())
                .withStorage(new SharedPreferenceStorage(this, "FIRSTAPP"))
                .create();
WebAuthClient webAuthSecondApp = new Okta.WebAuthBuilder()
                .withConfig(configSecondApp)
                .withContext(getApplicationContext())
                .withStorage(new SharedPreferenceStorage(this, "SECONDAPP"))
                .create();

if (true) { //provide option to login using different clients.
    webAuthFirstApp.registerCallback(...);
} else {
    webAuthSecondApp.registerCallback(...);
}
```

[activity]: https://developer.android.com/reference/android/app/Activity.html
[fragment-activity]: https://developer.android.com/reference/android/support/v4/app/FragmentActivity
[on-activity-result]: https://developer.android.com/reference/android/app/Activity.html#onActivityResult(int,%20int,%20android.content.Intent)
[session-token]: https://developer.okta.com/docs/reference/api/sessions/#session-token
[chrome-custom-tabs]: https://developer.chrome.com/multidevice/android/customtabs
[IDP]: https://developer.okta.com/docs/concepts/social-login/#features
[IDP-Google]: https://developer.okta.com/docs/guides/add-an-external-idp/google/before-you-begin/
[IDP-Facebook]: https://developer.okta.com/docs/guides/add-an-external-idp/facebook/before-you-begin/
[java-8-api]:https://developer.android.com/studio/write/java8-support#library-desugaring
