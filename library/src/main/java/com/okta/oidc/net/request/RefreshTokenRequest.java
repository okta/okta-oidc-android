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
package com.okta.oidc.net.request;

import androidx.annotation.RestrictTo;

import com.okta.oidc.util.AsciiStringListUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class RefreshTokenRequest extends TokenRequest {
    RefreshTokenRequest(HttpRequestBuilder b) {
        super(b);
    }

    protected Map<String, String> buildParameters(HttpRequestBuilder b) {
        scope = b.mTokenResponse.getScope();
        refresh_token = b.mTokenResponse.getRefreshToken();
        Map<String, String> params = new HashMap<>();
        params.put("client_id", client_id);
        params.put("grant_type", grant_type);
        params.put("refresh_token", refresh_token);
        params.put("scope", AsciiStringListUtil.iterableToString(Arrays.asList(scope)));
        return params;
    }
}
