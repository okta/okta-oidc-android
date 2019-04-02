# okta-oidc-android
OIDC SDK for Android

### onActivityResult override

ATTENTION! This lib use nested fragment and `onActivityResult` method to receive data from browser.
In case if you override onActivityResult method you must invoke `super.onActivityResult()` method.

```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
```