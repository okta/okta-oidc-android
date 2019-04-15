# okta-oidc-android
OIDC SDK for Android

### onActivityResult override

ATTENTION! This library uses a nested fragment and the `onActivityResult` method to receive data from the browser.
In the case that you override the 'onActivityResult' method you must invoke 'super.onActivityResult()' method.

```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
```