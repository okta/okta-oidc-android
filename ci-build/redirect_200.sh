#!/bin/bash

adb push app/src/androidTest/assets/mock.keystore.bks /sdcard/Download/

for i in {1..100}
do
 echo Test Number $i
 adb shell am instrument -w -r   -e debug false -e class 'com.okta.oidc.example.RedirectTest#redirectToApp200Response' com.okta.oidc.example.test/androidx.test.runner.AndroidJUnitRunner
done

