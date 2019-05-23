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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.Preconditions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BaseRequest<T, U extends AuthorizationException>
        implements HttpRequest<T, U> {
    HttpRequest.Type mRequestType;
    private static final String HTTPS_SCHEME = "https";
    private static final int HTTP_CONTINUE = 100;
    private volatile boolean mCanceled;
    protected HttpConnection mConnection;
    private HttpResponse mResponse;
    protected Uri mUri;
    private HttpURLConnection mUrlConn;

    public BaseRequest() {
    }

    @WorkerThread
    protected HttpResponse openConnection() throws IOException {
        Preconditions.checkArgument(HTTPS_SCHEME.equals(mUri.getScheme()),
                "only https connections are permitted");

        mUrlConn = mConnection.openConnection(new URL(mUri.toString()));
        mUrlConn.connect();

        if (mCanceled) {
            throw new IOException("Canceled");
        }

        boolean keepOpen = false;
        try {
            int responseCode = mUrlConn.getResponseCode();
            if (responseCode == -1) {
                throw new IOException("Invalid response code -1 no code can be discerned");
            }

            if (!hasResponseBody(responseCode)) {
                mResponse = new HttpResponse(responseCode, mUrlConn.getHeaderFields());
            } else {
                keepOpen = true;
                mResponse = new HttpResponse(
                        responseCode, mUrlConn.getHeaderFields(),
                        mUrlConn.getContentLength(), mUrlConn);
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
    public synchronized void close() {
        if (mResponse != null) {
            mResponse.disconnect();
            mResponse = null;
        }
        if (mUrlConn != null) {
            try {
                mUrlConn.getInputStream().close();
            } catch (IOException | NullPointerException e) {
                //NO-OP
            }
            mUrlConn.disconnect();
            mUrlConn = null;
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
