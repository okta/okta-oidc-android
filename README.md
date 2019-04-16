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

### Running AndroidTest

1. Make sure you have latest version of Chrome browser. Last version for success test is 73.0.3683.90
2. Install debug app on device
3. Clear cache in app and chrome browser using gradle command: app:clearData
4. Prepare device for UI testing using gradle command: app:prepareDeviceForUITesting
5. Run Android tests.