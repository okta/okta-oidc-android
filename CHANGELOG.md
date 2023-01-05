# 1.3.4
- [#347](https://github.com/okta/okta-oidc-android/pull/347) Use browser querying logic from okta-mobile-kotlin.

# 1.3.3
- [#341](https://github.com/okta/okta-oidc-android/pull/341) Validate TokenResponse before returning a Token to the user.

# 1.3.2
- [#328](https://github.com/okta/okta-oidc-android/pull/328) Allow HTTPS redirect URLs. Please note, not all user flows will result in a redirect back to the application if this is used.

# 1.3.1
- [#325](https://github.com/okta/okta-oidc-android/pull/325) Update transitive dependencies to preserve backwards compatibility (Make them api rather than implementation).

# 1.3.0
- [#321](https://github.com/okta/okta-oidc-android/pull/321) Update GSON to 2.9.0.

# 1.2.6

### Bug Fix
- [#319](https://github.com/okta/okta-oidc-android/pull/319) Cancelling a WebAuthClient now returns a cancelled response when the flow has been cancelled.

# 1.2.5

### Bug Fix
- [#316](https://github.com/okta/okta-oidc-android/pull/316) Fix a crash during login/logout.

# 1.2.4

### Bug Fix
- [#311](https://github.com/okta/okta-oidc-android/pull/311) Sleep and retry when crypto operations fail to fix a bug on Android 10.

# 1.2.3

### Bug Fix
- [#303](https://github.com/okta/okta-oidc-android/pull/303) Fix a crash during logout.

# 1.2.2

### Bug Fix
- [#293](https://github.com/okta/okta-oidc-android/pull/293) Fixes ambiguous error codes.

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
