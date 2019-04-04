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

import android.net.Uri;

import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.Preconditions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public abstract class BaseRequest<T, U extends AuthorizationException>
        implements HttpRequest<T, U> {
    protected HttpRequest.Type mRequestType;
    private static final String HTTPS_SCHEME = "https";
    private static final int HTTP_CONTINUE = 100;
    private volatile boolean mCanceled;
    protected HttpConnection mConnection;
    private HttpResponse mResponse;
    protected Uri mUri;

    public BaseRequest() {
    }

    @WorkerThread
    protected HttpResponse openConnection() throws IOException {
        Preconditions.checkArgument(HTTPS_SCHEME.equals(mUri.getScheme()),
                "only https connections are permitted");
        HttpURLConnection conn = mConnection.openConnection(new URL(mUri.toString()));
        conn.connect();
        if (mCanceled) {
            throw new IOException("Canceled");
        }
        boolean keepOpen = false;
        try {
            int responseCode = conn.getResponseCode();
            if (responseCode == -1) {
                throw new IOException("Invalid response code -1 no code can be discerned");
            }
            if (!hasResponseBody(responseCode)) {
                mResponse = new HttpResponse(responseCode, conn.getHeaderFields());
            } else {
                keepOpen = true;
                mResponse = new HttpResponse(
                        responseCode, conn.getHeaderFields(),
                        conn.getContentLength(), conn);
            }
            return mResponse;
        } finally {
            if (!keepOpen) {
                close();
            }
        }
    }

    @Override
    public void cancelRequest() {
        mCanceled = true;
        close();
    }

    @Override
    public void close() {
        if (mResponse != null) {
            mResponse.disconnect();
            mResponse = null;
        }
    }

    private boolean hasResponseBody(int responseCode) {
        return !(HTTP_CONTINUE <= responseCode && responseCode < HttpURLConnection.HTTP_OK)
                && responseCode != HttpURLConnection.HTTP_NO_CONTENT
                && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED;
    }

    @NonNull
    @Override
    public String toString() {
        return "RequestType=" + mRequestType +
                " URI=" + mUri;
    }
}