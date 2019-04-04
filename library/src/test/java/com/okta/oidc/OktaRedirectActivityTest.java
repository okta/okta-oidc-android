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

import android.content.Intent;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static com.okta.oidc.util.TestValues.CUSTOM_URL;
import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaRedirectActivityTest {

    @Test
    public void testForwardsRedirectToManagementActivity() {
        Uri uri = Uri.parse(CUSTOM_URL + "/redirect");
        Intent intent = new Intent();
        intent.setData(uri);

        ActivityController controller =
                Robolectric.buildActivity(OktaRedirectActivity.class, intent)
                        .create();

        OktaRedirectActivity redirectActivity = (OktaRedirectActivity) controller.get();

        Intent nextIntent = shadowOf(redirectActivity).getNextStartedActivity();
        assertThat(nextIntent).hasData(uri);
        assertThat(redirectActivity).isFinishing();
    }
}