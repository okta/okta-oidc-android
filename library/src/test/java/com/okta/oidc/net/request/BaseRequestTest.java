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

import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.HttpClientFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static com.okta.oidc.util.HttpClientFactory.USE_DEFAULT_HTTP;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 27)
public class BaseRequestTest {
    private BaseRequest<String, AuthorizationException> mRequest;
    private MockEndPoint mEndPoint;
    private OktaHttpClient mHttpClient;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    private HttpClientFactory mClientFactory;
    private final int mClientType;

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {USE_DEFAULT_HTTP},
                {HttpClientFactory.USE_OK_HTTP},
                {HttpClientFactory.USE_SYNC_OK_HTTP}});
    }

    public BaseRequestTest(int clientType) {
        mClientType = clientType;
    }

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        mRequest = new BaseRequest<String, AuthorizationException>() {
            @Override
            public String executeRequest(OktaHttpClient client) throws AuthorizationException {
                return null;
            }
        };
        mClientFactory = new HttpClientFactory();
        mClientFactory.setClientType(mClientType);
        mHttpClient = mClientFactory.build();
    }

    @Test
    public void openConnection() throws Exception {
        mEndPoint.enqueueReturnSuccessEmptyBody();
        mRequest.mUri = Uri.parse(mEndPoint.getUrl());
        mRequest.mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.GET)
                .create();
        HttpResponse response = mRequest.openConnection(mHttpClient);
        assertEquals(response.getStatusCode(), HTTP_OK);
    }

    @Test
    public void cancelRequest() throws Exception {
        mExpectedEx.expect(IOException.class);
        mExpectedEx.expectMessage("Canceled");
        mEndPoint.enqueueReturnSuccessEmptyBody(1);
        mRequest.mUri = Uri.parse(mEndPoint.getUrl());
        mRequest.mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.GET)
                .create();
        AtomicReference<Exception> exception = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                mRequest.openConnection(mHttpClient);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        t.start();

        mRequest.cancelRequest();
        t.join();
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    @Test
    public void close() throws Exception {
        mExpectedEx.expect(IOException.class);
        if (mClientType == USE_DEFAULT_HTTP) {
            mExpectedEx.expectMessage("stream is closed");
        } else {
            mExpectedEx.expectMessage("closed");
        }

        mEndPoint.enqueueConfigurationSuccess();
        mRequest.mUri = Uri.parse(mEndPoint.getUrl());
        mRequest.mConnParams = new ConnectionParameters.ParameterBuilder()
                .setRequestMethod(ConnectionParameters.RequestMethod.GET)
                .create();
        HttpResponse response = mRequest.openConnection(mHttpClient);
        mRequest.close();
        response.getContent().read();
    }
}