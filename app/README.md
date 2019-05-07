# Okta Sample App

## Overview

The sample is mainly used to exercise the SDK functionality. It is not a fully featured example of using the SDK. The sample app can be used as a convenient reference point from which to develop your own application.

## Getting Started

To run the example project, clone the repo and run `./gradlew assemble` from the root directory.

You can create an Okta developer account at [https://developer.okta.com/](https://developer.okta.com/).

1. After login, from the Admin dashboard, navigate to **Applications**&rarr;**Add Application**
2. Choose **Native** as the platform
3. Populate your new Native OpenID Connect application with values similar to:

| Setting              | Value                                               |
| -------------------- | --------------------------------------------------- |
| Application Name     | Native OpenId Connect App *(must be unique)*        |
| Login URI            | com.okta.oidc.example:/callback                     |
| End Session URI      | com.okta.oidc.example:/logout                       |
| Allowed grant types  | Authorization Code, Refresh Token *(recommended)*   |

4. Click **Finish** to redirect back to the *General Settings* of your application.
5. Copy the **Client ID**, as it will be needed for the client configuration.
6. Get your issuer, which is a combination of your Org URL (found in the upper right of the console home page). For example, https://dev-1234.oktapreview.com/.

**Note:** *As with any Okta application, make sure you assign Users or Groups to the application. Otherwise, no one can use it.*

### Update the URI Scheme

In order to redirect back to your application from a web browser, you must specify a unique URI to
your app. To do this, you must define a gradle manifest placeholder in your app's `build.gradle`:

```gradle
android.defaultConfig.manifestPlaceholders = [
    "appAuthRedirectScheme": "com.okta.oidc.example"
]
```

## Running AndroidTest

Make sure you have latest version of Chrome browser. Latest version for a successful test is 73.0.3683.90

Add your username and password to local.properties

```bash
test.username=yourUsername
test.password=yourPassword
```

Install debug app on device then clear cache in the app and chrome browser by running gradle command:

```gradle
./gradlew app:clearData
```

Prepare device for UI testing by running gradle command:

```gradle
./gradlew app:prepareDeviceForUITesting
```

Run the tests by running gradle command:

```gradle
./gradlew app:cAT
```
