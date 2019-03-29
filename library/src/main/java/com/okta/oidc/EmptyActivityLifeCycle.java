/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.oidc;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/*
Empty implementation
 */
class EmptyActivityLifeCycle implements Application.ActivityLifecycleCallbacks {
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        //NO-OP
    }

    @Override
    public void onActivityStarted(Activity activity) {
        //NO-OP
    }

    @Override
    public void onActivityResumed(Activity activity) {
        //NO-OP
    }

    @Override
    public void onActivityPaused(Activity activity) {
        //NO-OP
    }

    @Override
    public void onActivityStopped(Activity activity) {
        //NO-OP
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        //NO-OP
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        //NO-OP
    }
}