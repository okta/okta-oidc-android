[<img src="https://devforum.okta.com/uploads/oktadev/original/1X/bf54a16b5fda189e4ad2706fb57cbb7a1e5b8deb.png" align="right" width="256px"/>](https://devforum.okta.com/)
[![CI Status](http://img.shields.io/travis/okta/okta-oidc-android.svg?style=flat)](https://travis-ci.org/okta/okta-oidc-android)
[![Download](https://api.bintray.com/packages/okta/com.okta.android/okta-oidc-android/images/download.svg) ](https://bintray.com/okta/com.okta.android/okta-oidc-android/_latestVersion)

# Okta OpenID Connect & OAuth 2.0 Library

## Table of Contents

- [Overview](#Overview)
  - [Requirements](#Requirements)
  - [Installation](#Installation)
  - [Sample app](#Sample-app)
- [Configuration](#Configuration)
  - [Using JSON configuration file](#Using-JSON-configuration-file)
- [Sign in with a browser](#Sign-in-with-a-browser)
  - [onActivityResult override](#onActivityResult-override)
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
- [Advanced techniques](#Advanced-techniques)
  - [Sign in with a sessionToken (Async)](#Sign-in-with-a-sessionToken-(Async))
  - [Sign in with a sessionToken (Sync)](#Sign-in-with-a-sessionToken-(Sync))
  - [Multiple Authorization Clients](#Multiple-authorization-clients)

## Overview

This library is for communicating with Okta as an OAuth 2.0 + OpenID Connect provider, and follows current best practice for native apps using [Authorization Code Flow + PKCE](https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce).

You can learn more on the [Okta + Android](https://developer.okta.com/code/android/) page in our documentation. For more information about [Okta OpenID Connect & OAuth 2.0 API](https://developer.okta.com/docs/api/resources/oidc/).

### Requirements

Okta OIDC SDK supports Android API 19 and above. [Chrome custom tab](https://developer.chrome.com/multidevice/android/customtabs) enabled browsers
are needed by the library for browser initiated authorization. App must use FragmentActivity or any extensions of it to work with the library. An Okta developer account is needed to run the sample.

### Installation

Add the `Okta OIDC` dependency to your `build.gradle` file:

```gradle
implementation 'com.okta.oidc.android:okta-oidc-androidx:1.0.0'
```

### Sample app

A sample is contained within this repository. For more information on how to
build, test and configure the sample, see the sample [README](https://github.com/okta/okta-oidc-android/blob/master/app/README.md).

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
                .withStorage(new SimpleOktaStorage(this))
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
        } else if (status == AuthorizationStatus.IN_PROGRESS) {
            //authorization is in progress.
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

The `client` can now be used to authenticate users and authorizing access.

### Using JSON configuration file

You can also create a `config` by poviding a JSON file.
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
    .withJsonFile(this, R.id.okta_oidc_config)
    .create();
```

**Note**: To receive a **refresh_token**, you must include the `offline_access` scope.

## Sign in with a browser

The authorization flow consists of four stages.

1. Service discovery - This uses the discovery uri to get a list of endpoints.
2. Authorizing the user with crome custom tabs to obtain an authorization code.
3. Exchanging the authorizaton code for a access token, ID token, and refresh token.
4. Using the tokens to interact with a resource server for access to user data.

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

ATTENTION! This library uses a nested fragment and the `onActivityResult` method to receive data from the browser.
In the case that you override the 'onActivityResult' method you must invoke 'super.onActivityResult()' method.

```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
```

## Sign in with your own UI

If you would like to use your own in-app user interface instead
of the web browser you can do by using a `sessionToken`:

```java
AuthClient authClient = new Okta.AuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .create();
```

After building the `AuthClient` you should call `signIn` method where you need to provide a `sessionToken` and `RequestCallback`

```java
SessionClient sessionClient = authClient.getSessionClient();
if (!sessionClient.isAuthenticated()) {
    authClient.signIn("{sessionToken}", null, new RequestCallback<AuthorizationResult, AuthorizationException>() {
        @Override
        public void onSuccess(@NonNull AuthorizationResult result) {
            //client is now authorized.
            Tokens tokens = sessionClient.getTokens();
        }

        @Override
        public void onError(String error, AuthorizationException exception) {
            //handle error
        }
    });
}

```

**Note**: To get a **sessionToken**, you must use [Okta's Authentication API](https://developer.okta.com/docs/api/resources/authn/#application-types). You can use [Okta Java Authentication SDK](https://github.com/okta/okta-auth-java) to get a `sessionToken`. An example of using the Authentication API can be found [here](https://github.com/okta/samples-android/tree/master/custom-sign-in).

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

Tokens are still active (unless expired) even if you have cleared the browser session. An optional step is to revoke the tokens to make them in-active. Please see [Revoke the tokens](#Revoking-a-Token).

### Clear tokens from device

Tokens can be removed from the device by simply calling:

```java
    client.getSessionClient().clear();
```

After this the user is signed out.

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

In addition to the built in endpoints, you can use the client interface to perform your own authorized requests, whatever they might be. You can call `authorizedRequest` requests and have the access token automatically added to the `Authorization` header with the standard OAuth 2.0 prefix of `Bearer`.

```java
final Uri uri;
HashMap<String, String> properties = new HashMap<>();
properties.put("queryparam", "queryparam");
HashMap<String, String> postParameters = new HashMap<>();
postParameters.put("postparam", "postparam");

client.getSessionClient().authorizedRequest(uri, properties,
                postParameters, HttpConnection.RequestMethod.POST, new RequestCallback<JSONObject, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull JSONObject result) {
        //handle JSONObject result.
    }

    @Override
    public void onError(String error, AuthorizationException exception) {
        //handle failed request
    }
});
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
```

**Note:** *Access, refresh and ID tokens need to be revoked in separate requests. The request only revokes the specified token*

### Introspect a token

Tokens can be checked for more detailed information by using the introspect endpoint:

```java
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

`WebAuthClient` redirects to a chrome custom tabs enabled browser for authenticaiton.
The following shows how to create a asynchronous web authentication client.

```java
WebAuthClient webAuthClient = new Okta.WebAuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
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
        .withStorage(new SimpleOktaStorage(this))
        .withTabColor(Color.BLUE)
        .supportedBrowsers("com.android.chrome", "com.google.android.apps.chrome", "com.android.chrome.beta")
        .create();
```

### AuthClient

`AuthClient` will require a `sessionToken`. See [Sign in with your own UI](#Sign-in-with-your-own-UI) for more information on how to obtain a `sessionToken`.
The following shows how to create a asynchronous authentication client:

```java
AuthClient authClient = new Okta.AuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
        .withCallbackExecutor(Executors.newSingleThreadExecutor())
        .create();
```

### SyncAuthClient

The following shows how to create synchronous authentication client:

```java
SyncAuthClient syncAuthClient = new Okta.SyncAuthBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
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
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

The library will attempt to use FireFox then Samsung browsers first.
If none are found it will default to Chrome.

**Note**: The library only supports [Chrome custom tab](https://developer.chrome.com/multidevice/android/customtabs) enabled browsers. If no compatible browsers are found you'll receive a `AuthorizationException` with a `No compatible browser found` message. You should handle this error by redirecting the user to download a compatible browser in the app store.

### Customize HTTP requests

You can customize how HTTP connections are made by implementing the `HttpConnectionFactory` interface. For example if you want to customize the SSL socket factory:

```java
private class MyConnectionFactory implements HttpConnectionFactory {
    @Override
    public HttpURLConnection build(@NonNull URL url) throws Exception {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustManager, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });
        return (HttpURLConnection) url.openConnection();
    }
}

client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .withHttpConnectionFactory(new MyConnectionFactory())
    .create();
```

### Storage

The library provides storage using shared preferences. If you wish to use SQL or any other storage mechanism you can implement the storage interface and use it when creating the various `AuthClient`.
The default storage also requires a hardware-backed keystore for encryption. If the device does not provide hardware-backed keystore the library will not store any data. If you wish to override this behavior you can implement or extend the `OktaStorage` interface:

```java
public class MyStorage implements OktaStorage {
    @Override
    public void save(@NonNull String key, @NonNull String value) {
        //Provide implementation
    }

    @Nullable
    @Override
    public String get(@NonNull String key) {
        return null; //Provide implementation
    }

    @Override
    public void delete(@NonNull String key) {
        //Provide implementation
    }

    @Override
    public boolean requireHardwareBackedKeyStore() {
        //return true if hardware backed keystore is required
        //return false if hardware backed keystore is not required
    }
}

client = new Okta.WebAuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new MyStorage())
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

### Encryption

Encryption is applied to all data that is stored by the library. You can specify your own encryption algorithm with the following steps:

1. Build your own implementation of `EncryptionManager`
2. Provide it within selected Okta Client Builder

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

### Hardware-backed keystore

The default `EncryptionManager` provides a check to see if the device supports hardware-backed keystore. If you implement your own `EncryptionManager` you'll have to implement this check. You can return `true` to tell the default storage that the device have a hardware-backed keystore. The [storage](#Storage) and [encrytion](#Encryption) mechanisms work together to ensure that data is stored securely.

## Advanced techniques

Sometimes as a developer you want to have more control over SDK and here is a couple of advanced API's that are available to give
you more control as a developer.

### Sign in with a sessionToken (Async)

In order to use authentication flow without browser you can use our `AuthClient`

```Java
AuthClient authClient = new Okta.AuthBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .create();
```

After building `AuthClient` you should call `signIn` method where you need provide `sessionToken` and `RequestCallback`

```java
authClient.signIn("{sessionToken}", null, new RequestCallback<AuthorizationResult, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull AuthorizationResult result) {

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
    .withStorage(new SimpleOktaStorage(this))
    .create();
```

After building `SyncAuthClient` you should call `signIn` method where you need provide `sessionToken`
NOTE: that is a synchronous call so please check that it is not performed on Ui Thread.

```java
syncAuthClient.signIn("sessionToken", null)
```

Optionally you can provide `AuthenticationPayload` as a part of sign in call.

### Multiple authorization clients

Multiple `AuthClient` are supported. However for `WebAuthClient` only one callback can be registered. For example you can have multiple authorization servers redirecting to the same application:

```java
OIDCConfig configFirstApp = new OIDCConfig.Builder()
    .withJsonFile(this, R.id.okta_oidc_config_first)
    .create();

//config file with different domain, client_id than config_first but same redirect_uri
OIDCConfig configSecondApp = new OIDCConfig.Builder()
    .withJsonFile(this, R.id.okta_oidc_config_second)
    .create();

WebAuthClient webAuthFirstApp = new Okta.WebAuthBuilder()
                .withConfig(configFirstApp)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this, "FIRSTAPP"))
                .create();
WebAuthClient webAuthSecondApp = new Okta.WebAuthBuilder()
                .withConfig(configSecondApp)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this, "SECONDAPP"))
                .create();

if (true) { //provide option to login using different clients.
    webAuthFirstApp.registerCallback(...);
} else {
    webAuthSecondApp.registerCallback(...);
}
```
