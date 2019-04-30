# Okta OpenID Connect & OAuth 2.0 API library

## Overview

This library currently supports:

[OpenID Connect & OAuth 2.0 API](https://developer.okta.com/docs/api/resources/oidc/)

## Requirements

Okta OIDC SDK supports Android API 19 and above. [Chrome custom tab](https://developer.chrome.com/multidevice/android/customtabs) enabled browsers
are needed by the library for browser initiated authorization. App must use FragmentActivity or any extensions of it to work with the library. An Okta developer account is needed to run the sample.

## Installation

Add the `Okta OIDC` dependency to your `build.gradle` file:

```gradle
implementation 'com.okta.oidc.android:okta-oidc-androidx:1.0.0'
```

## Sample app

A sample is contained within this repository. For more information on how to
build, test and configure the sample, see the sample [README](https://github.com/okta/okta-oidc-android/blob/master/app/README.md).

## Configuration

First the Authenticate client must have a config to interact with Okta's OIDC provider. Create a `OIDCConfig` like the following example:

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
AsyncWebAuth client = new Okta.AsyncWebBuilder()
                .withConfig(config)
                .withContext(getApplicationContext())
                .withStorage(new SimpleOktaStorage(this))
                .create();
```

After creating the client, register a callback to receive authorization results.

```java
client.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull AuthorizationStatus status) {
        if (status == AuthorizationStatus.AUTHORIZED) {
            //client is authorized.
            Tokens tokens = client.getTokens();
        } else if (status == AuthorizationStatus.LOGGED_OUT) {
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
    .withConfig(this, R.id.okta_oidc_config)
    .create();
```

**Note**: To receive a **refresh_token**, you must include the `offline_access` scope.

## Sign in with a browser

The authorization flow consists of four stages.

1. Service discovery - This uses the discovery uri to get a list of endpoints.
2. Authorizing the user with crome custom tabs to obtain an authorization code.
3. Exchanging the authorizaton code for a access token, ID token, and refresh token.
4. Using the tokens to interact with a resource server for access to user data.

This is all done in the background by the SDK. For example to login you can call:

```java
client.logIn(this, null);
```

The results will be returned in the registered callback. If the application needs to send extra
data to the api endpoint, `AuthenticationPayload` can be used:

```java
AuthenticationPayload payload = new AuthenticationPayload.Builder()
    .setLoginHint("youraccount@okta.com")
    .addParameter("max_age", "5000")
    .build();

client.logIn(this, payload);
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
AsyncNativeClient syncNativeClient = new Okta.SyncNativeBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .create();
```

After building `AsyncNativeClient` you should call `login` method where you need provide `sessionToken` and `RequestCallback`

```java
asyncNativeClient.logIn("{sessionToken}", null, new RequestCallback<AuthorizationResult, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull AuthorizationResult result) {

    }

    @Override
    public void onError(String error, AuthorizationException exception) {

    }
});
```

**Note**: To get a **sessionToken**, you must use [Okta's Authentication API](https://developer.okta.com/docs/api/resources/authn/#application-types). You can use [Okta Java Authentication SDK](https://github.com/okta/okta-auth-java) to get a `sessionToken`. An example of using the Authentication API can be found [here](https://github.com/okta/samples-android/tree/master/custom-sign-in).

## Sign out

If the user is signed in using the browser initiated authorization flow, then logging out
is a two or three step process depending on revoking the tokens.

1. Clear the browser session.
2. [Revoke the tokens](#Revoking-a-Token) (optional)
3. Clear the app session (stored tokens) in [memory](#Token-Management).

If the user is signed in using a [sessionToken](#Sign-in-with-your-own-UI) you can skip clearing the browser.

### Clear browser session

In order to clear the browser session you have to call `signOutFromOkta()`.

```java
    client.signOutFromOkta(this);
}
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

After this the user is logged out.

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

### Performing Authorized Requests

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
client.getSessionClient().revokeToken(client.getTokens().getRefreshToken(),
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
    TokenTypeHint.REFRESH_TOKEN, new RequestCallback<IntrospectResponse, AuthorizationException>() {
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

## Token Management

Tokens are encrypted and securely stored in the private Shared Preferences.
If you do not want `AuthenticateClient` to store the data you can pass in a empty interface when creating the `client`

```java

client = new Okta.AsyncWebBuilder()
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

## Variants of client

You can create client which do login via web in async way

```java
AsyncWebAuth mWebOktaAuth = new Okta.AsyncWebBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
        .withCallbackExecutor(Executors.newSingleThreadExecutor())
        .withTabColor(Color.BLUE)
        .supportedBrowsers(new String[]{"com.android.chrome", "org.mozilla.firefox"})
        .create();
```

You can create client which do login using sessionToken in async way

```java
AsyncNativeAuth mNativeOktaAuth = new Okta.AsyncNativeBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
        .withCallbackExecutor(Executors.newSingleThreadExecutor())
        .create();
````

You can create client which do login via web in sync way

```java
SyncWebAuth mWebSyncOktaAuth = new Okta.SyncWebBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
        .withTabColor(Color.BLUE)
        .supportedBrowsers("com.android.chrome", "com.google.android.apps.chrome", "com.android.chrome.beta")
        .create();
```

You can create client which do login using sessionToken in sync way
```java
SyncNativeAuth mNativeSyncOktaAuth = new Okta.SyncNativeBuilder()
        .withConfig(config)
        .withContext(getApplicationContext())
        .withStorage(new SimpleOktaStorage(this))
        .create();
```

## Providing browser used for authorization

The default browser used for authorization is Chrome. If you want to change it FireFox, you can add this in the `AuthenticateClient.Builder()`:

```java

String SAMSUNG = "com.sec.android.app.sbrowser";
String FIREFOX = "org.mozilla.firefox";

client = new Okta.AsyncWebBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

The library will attempt to use FireFox then Samsung browsers first.
If none are found it will default to Chrome.

## Customize HTTP requests

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

client = new Okta.AsyncWebBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .withHttpConnectionFactory(new MyConnectionFactory())
    .create();
```

## Providing custom storage

The library uses a simple storage using shared preferences to store data. If you wish to use SQL or any other storage mechanism you can implement the storage interface and use it when creating `AuthenticateClient`.

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
}

client = new Okta.AsyncWebBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new MyStorage())
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

## Providing custom encryption

TODO add after encryption and smart lock branch is merged.

## Advanced techniques

Sometimes as a developer you want to have more control over SDK and here is a couple of advanced API's that are available to give
you more control as a developer.

### Login with a sessionToken (Async)

In order to use native authentication flow without browser you can use our `AsyncNativeClient`

```Java
AsyncNativeClient asyncNativeClient = new Okta.AsyncNativeBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .create();
```

After building `AsyncNativeClient` you should call `login` method where you need provide `sessionToken` and `RequestCallback`

```java
asyncNativeClient.logIn("sessionToken", null, new RequestCallback<AuthorizationResult, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull AuthorizationResult result) {

    }

    @Override
    public void onError(String error, AuthorizationException exception) {

    }
});
```

Optionally you can provide `AuthenticationPayload` as a part of login call.

### Native login (Sync)

In order to use native authentication flow without browser you can use our `SyncNativeClient`

```java
syncNativeClient = new Okta.SyncNativeBuilder()
    .withConfig(config)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .create();
```

After building `AsyncNativeClient` you should call `login` method where you need provide `sessionToken`
NOTE: that is a synchronous call so please check that it is not performed on Ui Thread 

```java
syncNativeClient.logIn("sessionToken", null)
```

Optionally you can provide `AuthenticationPayload` as a part of login call.
