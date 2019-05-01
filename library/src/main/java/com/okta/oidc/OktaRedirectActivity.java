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
import android.content.Intent;
import android.os.Bundle;

/**
 * This activity receives the redirect URI sent by the {@code /authorize} endpoint.
 * It sends the data received from the redirect to {@link OktaAuthenticationActivity}.
 *
 * @see "Authorization Code with PKCE flow
 * <https://developer.okta.com/authentication-guide/auth-overview/#authorization-code-with-pkce-flow>"
 * @see "Implementing the Authorization Code with PKCE flow
 * <https://developer.okta.com/authentication-guide/implementing-authentication/auth-code-pkce/>"
 */
public class OktaRedirectActivity extends Activity {
    public static final String REDIRECT_ACTION = OktaRedirectActivity.class.getCanonicalName()
            + ".REDIRECT_ACTION";

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        Intent intent = new Intent(this, OktaAuthenticationActivity.class);
        intent.setAction(REDIRECT_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setData(getIntent().getData());
        startActivity(intent);
        finish();
    }
}
