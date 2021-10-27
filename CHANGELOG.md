# 1.2.1

### Bug Fix
- [#288](https://github.com/okta/okta-oidc-android/pull/288) Fix for ActivityNotFoundException.

# 1.2.0

### Feature
- [#281](https://github.com/okta/okta-oidc-android/pull/281) Add ability to customize ID Token validation.

# 1.1.0

### Feature
- [#276](https://github.com/okta/okta-oidc-android/pull/276) Adds support for parallel refresh token requests.
- [#277](https://github.com/okta/okta-oidc-android/pull/277) Run SessionClient requests in serial.

### Bug Fix
- [#272](https://github.com/okta/okta-oidc-android/pull/272) Make `OktaAuthenticationActivity` `launchMode` `singleTop` which fixes an issue where the browser tab would remain in the Android recents list.

# 1.0.20

### Bug Fix

- [#244](https://github.com/okta/okta-oidc-android/pull/244) Fix unmarshall exception during onCreate on some samsung devices.

### Other

- [#245](https://github.com/okta/okta-oidc-android/pull/245) Update error code to remove duplicate, and follow pattern of other error codes.
- [#246](https://github.com/okta/okta-oidc-android/pull/246) Update build and 3rd party dependencies.

# 1.0.19

### Bug Fix

- [#230](https://github.com/okta/okta-oidc-android/pull/230) Fixes androidx lifecycle compliance

### Other 

- [#242](https://github.com/okta/okta-oidc-android/pull/242) Publish to maven central rather than jcenter.
- Remove support for non androidx variant.
- Changed artifact name from `com.okta.android:oidc-androidx` to `com.okta.android:okta-oidc-android`
