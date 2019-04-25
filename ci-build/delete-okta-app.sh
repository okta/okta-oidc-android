#!/bin/bash
# Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
# The Okta software accompanied by this notice is provided pursuant to the Apache License, Version 2.0 (the "License.")
#
# You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#
# See the License for the specific language governing permissions and limitations under the License.

# Get the package name and parse it 
packageName=$($ANDROID_HOME/platform-tools/adb shell pm list packages | grep "com.okta.oidc.example" | head -n 1 | cut -b 9- | tr -d "\r")

if [ -z "$packageName" ]; then
    echo "Okta app with package name that starts with com.okta.oidc.example not found on device."
else
    echo "Trying to uninstall okta app with package name $packageName"
    $ANDROID_HOME/platform-tools/adb uninstall $packageName
fi
