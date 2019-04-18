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

import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class BaseRequestTest {
    private BaseRequest<String, AuthorizationException> mRequest;
    private ExecutorService mCallbackExecutor;
    private MockEndPoint mEndPoint;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        mCallbackExecutor = Executors.newSingleThreadExecutor();
        mRequest = new BaseRequest<String, AuthorizationException>() {
            @Override
            public void dispatchRequest(RequestDispatcher dispatcher, RequestCallback callback) {
                //NO-OP
            }

            @Override
            public String executeRequest() throws AuthorizationException {
                return null;
            }
        };
    }

    @Test
    public void openConnection() throws IOException {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        mRequest.mUri = Uri.parse(mEndPoint.getUrl());
        mRequest.mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .create();
        HttpResponse response = mRequest.openConnection();
        assertEquals(response.getStatusCode(), HTTP_OK);
    }

    @Test
    public void cancelRequest() throws IOException {
        mExpectedEx.expect(IOException.class);
        mExpectedEx.expectMessage("Canceled");
        mEndPoint.enqueueReturnSuccessEmptyBody();
        mRequest.mUri = Uri.parse(mEndPoint.getUrl());
        mRequest.mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .create();
        mRequest.cancelRequest();
        mRequest.openConnection();
    }

    @Test
    public void close() throws IOException {
        mExpectedEx.expect(IOException.class);
        mExpectedEx.expectMessage("stream is closed");
        mEndPoint.enqueueConfigurationSuccess();
        mRequest.mUri = Uri.parse(mEndPoint.getUrl());
        mRequest.mConnection = new HttpConnection.Builder()
                .setRequestMethod(HttpConnection.RequestMethod.GET)
                .create();
        HttpResponse response = mRequest.openConnection();
        mRequest.close();
        response.getContent().read();
    }
}