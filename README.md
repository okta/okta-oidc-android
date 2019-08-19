# Okta Android Authentication

## Overview

This branch provides a set of libraries from [Okta Java Authentication SDK](https://github.com/okta/okta-auth-java) that are compatible with Android devices that doesn't have Java8 support (API < 24). These libraries are created using a tool called desugar from bazel. The primary use case for these libraries is for implementing a custom sign-in UI instead of using chrome custom tabs.

## Installation

Get the libraries by cloning the specified branch:

```bash
git clone -b authn_android git@github.com:okta/okta-oidc-android.git auth_android
```

### Gradle changes

Copy the authn libraries from `tools/authn/libs` to your projects `libs` directory.
In your `build.gradle` file add the following:

```gradle
implementation fileTree(dir: 'libs', include: ['*.jar'])
implementation 'com.okta.android:oidc-androidx:1.0.2'
//okta-authn-java dependencies
implementation group: 'com.fasterxml.jackson.core', name: 'jackson-annotations', version: '2.9.8'
implementation group: 'com.fasterxml.jackson.core', name: 'jackson-databind', version: '2.9.8'
implementation group: 'org.slf4j', name: 'slf4j-api', version: '1.7.25'
implementation group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.25'
implementation group: 'com.squareup.okhttp3', name: 'okhttp', version: '3.11.0'
```

## Limitations

Java8 `default` interface methods are not properly converted unless the application is also built using bazel. The methods with `default` keyword from [AuthenticationClient](https://developer.okta.com/okta-auth-java/development/apidocs/com/okta/authn/sdk/client/AuthenticationClient.html) is not supported. The [DefaultAuthenticationClient](https://developer.okta.com/okta-auth-java/development/apidocs/com/okta/authn/sdk/impl/client/DefaultAuthenticationClient.html) have methods that calls the `default` implementation so these are not supported.

Instead of using the `default` methods:

```java
//Not supported in API < 24
mAuthenticationClient.authenticate(username, password.toCharArray(),null, handler);
```

Use the following:

```java
AuthenticationRequest request = mAuthenticationClient.instantiate(AuthenticationRequest.class)
    .setUsername(username)
    .setPassword(password.toCharArray())
    .setRelayState(null);

mAuthenticationClient.authenticate(request, null, handler);
```

Below is a list of `DefaultAuthenticationClient` APIs that should be used instead of the `default` one:

```java
AuthenticationResponse authenticate(AuthenticationRequest request, RequestContext requestContext, AuthenticationStateHandler stateHandler);

AuthenticationResponse changePassword(ChangePasswordRequest changePasswordRequest, RequestContext requestContext, AuthenticationStateHandler stateHandler)

AuthenticationResponse resetPassword(ChangePasswordRequest request, RequestContext requestContext, AuthenticationStateHandler stateHandler)

AuthenticationResponse unlockAccount(UnlockAccountRequest request, RequestContext requestContext, AuthenticationStateHandler stateHandler)

AuthenticationResponse enrollFactor(FactorEnrollRequest request, RequestContext requestContext, AuthenticationStateHandler stateHandler)
```
