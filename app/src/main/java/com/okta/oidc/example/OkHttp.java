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
import androidx.annotation.WorkerThread;

import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.OktaHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.okta.oidc.net.ConnectionParameters.CONTENT_TYPE;


/**
 * A OktaHttpClient implementation using OkHttpClient.
 */
public class OkHttp implements OktaHttpClient {
    private static final String TAG = "OkHttp";
    /**
     * The constant sOkHttpClient.
     */
    protected static OkHttpClient sOkHttpClient;
    /**
     * The Call.
     */
    protected volatile Call mCall;
    /**
     * The response.
     */
    protected Response mResponse;
    /**
     * The exception.
     */
    protected Exception mException;


    /**
     * Build request request.
     *
     * @param uri   the uri
     * @param param the param
     * @return the request
     */
    protected Request buildRequest(Uri uri, ConnectionParameters param) {
        if (sOkHttpClient == null) {
            sOkHttpClient = new OkHttpClient.Builder()
                    .readTimeout(param.readTimeOutMs(), TimeUnit.MILLISECONDS)
                    .connectTimeout(param.connectionTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();
        }
        Request.Builder requestBuilder = new Request.Builder().url(uri.toString());
        for (Map.Entry<String, String> headerEntry : param.requestProperties().entrySet()) {
            String key = headerEntry.getKey();
            requestBuilder.addHeader(key, headerEntry.getValue());
        }
        if (param.requestMethod() == ConnectionParameters.RequestMethod.GET) {
            requestBuilder = requestBuilder.get();
        } else {
            Map<String, String> postParameters = param.postParameters();
            if (postParameters != null) {
                FormBody.Builder formBuilder = new FormBody.Builder();
                for (Map.Entry<String, String> postEntry : postParameters.entrySet()) {
                    String key = postEntry.getKey();
                    formBuilder.add(key, postEntry.getValue());
                }
                RequestBody formBody = formBuilder.build();
                requestBuilder.post(formBody);
            } else if (param.getBody() != null) {
                MediaType type = MediaType.parse(param.requestProperties().get(CONTENT_TYPE));
                requestBuilder.post(RequestBody.create(type, param.getBody()));
            } else {
                requestBuilder.post(RequestBody.create(null, ""));
            }
        }
        return requestBuilder.build();
    }

    @Override
    @WorkerThread
    public InputStream connect(@NonNull Uri uri, @NonNull ConnectionParameters param)
            throws Exception {
        Request request = buildRequest(uri, param);
        mCall = sOkHttpClient.newCall(request);
        final CountDownLatch latch = new CountDownLatch(1);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mException = e;
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) {
                mResponse = response;
                latch.countDown();
            }
        });
        latch.await();
        if (mException != null) {
            throw mException;
        }
        if (mResponse != null && mResponse.body() != null) {
            return mResponse.body().byteStream();
        }
        return null;
    }

    @Override
    public void cleanUp() {
        //NO-OP
    }

    @Override
    public void cancel() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        if (mResponse != null) {
            return mResponse.headers().toMultimap();
        }
        return null;
    }

    @Override
    public String getHeader(String header) {
        if (mResponse != null) {
            return mResponse.header(header);
        }
        return null;
    }

    @Override
    public int getResponseCode() throws IOException {
        if (mResponse != null) {
            return mResponse.code();
        }
        return -1;
    }

    @Override
    public int getContentLength() {
        if (mResponse != null && mResponse.body() != null) {
            return (int) mResponse.body().contentLength();
        }
        return -1;
    }

    @Override
    public String getResponseMessage() throws IOException {
        if (mResponse != null) {
            return mResponse.message();
        }
        return null;
    }
}
