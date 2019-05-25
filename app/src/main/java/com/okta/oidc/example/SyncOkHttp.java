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

package com.okta.oidc.example;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.okta.oidc.net.ConnectionParameters;

import java.io.InputStream;

import okhttp3.Request;

/**
 * A OktaHttpClient implementation using OkHttpClient. This will use synchronous call.
 */
public class SyncOkHttp extends OkHttp {
    @Override
    public InputStream connect(@NonNull Uri uri, @NonNull ConnectionParameters param)
            throws Exception {
        Request request = buildRequest(uri, param);
        mCall = sOkHttpClient.newCall(request);
        mResponse = mCall.execute();
        if (mResponse != null && mResponse.body() != null) {
            return mResponse.body().byteStream();
        }
        return null;
    }
}
