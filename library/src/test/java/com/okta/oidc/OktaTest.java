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

import android.content.Context;
import android.graphics.Color;

import androidx.test.platform.app.InstrumentationRegistry;

import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.clients.SyncAuthClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.clients.SyncAuthClientFactoryImpl;
import com.okta.oidc.clients.web.SyncWebAuthClient;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.TestValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class OktaTest {

    private OIDCConfig mAccount;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private HttpConnectionFactory mConnectionFactory;
    private OktaStorage mStorage;
    private Context mContext;
    private int tabColor;
    private String[] supportBrowsers;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mStorage = new SimpleOktaStorage(mContext);
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();

        String url = new MockEndPoint().getUrl();
        mAccount = TestValues.getAccountWithUrl(url);

        tabColor = Color.BLACK;
        supportBrowsers = new String[]{""};
    }


    @Test
    public void testAsyncWebBuilder() {
        Okta.AsyncWebBuilder builder = mock(Okta.AsyncWebBuilder.class);
        WebAuthClient otherClient = new Okta.AsyncWebBuilder()
                .withConfig(mAccount)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .withCallbackExecutor(mExecutor)
                .withTabColor(tabColor)
                .supportedBrowsers(supportBrowsers)
                .create();
        when(builder.create()).thenReturn(otherClient);

        builder.withConfig(mAccount);
        verify(builder).withConfig(mAccount);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        builder.withCallbackExecutor(mExecutor);
        verify(builder).withCallbackExecutor(mExecutor);

        builder.withTabColor(tabColor);
        verify(builder).withTabColor(tabColor);

        builder.supportedBrowsers(supportBrowsers);
        verify(builder).supportedBrowsers(supportBrowsers);

        Object client = builder.create();
        verify(builder).create();
        assertEquals(otherClient, client);
    }

    @Test
    public void testAsyncNativeBuilder() {
        Okta.AsyncNativeBuilder builder = mock(Okta.AsyncNativeBuilder.class);
        AuthClient otherClient = new Okta.AsyncNativeBuilder()
                .withConfig(mAccount)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .withCallbackExecutor(mExecutor)
                .create();
        when(builder.create()).thenReturn(otherClient);

        builder.withConfig(mAccount);
        verify(builder).withConfig(mAccount);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        builder.withCallbackExecutor(mExecutor);
        verify(builder).withCallbackExecutor(mExecutor);

        Object client = builder.create();
        verify(builder).create();
        assertEquals(otherClient, client);
    }

    @Test
    public void testSyncWebBuilder() {
        Okta.SyncWebBuilder builder = mock(Okta.SyncWebBuilder.class);
        SyncWebAuthClient otherClient = new Okta.SyncWebBuilder()
                .withConfig(mAccount)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .withTabColor(tabColor)
                .supportedBrowsers(supportBrowsers)
                .create();
        when(builder.create()).thenReturn(otherClient);

        builder.withConfig(mAccount);
        verify(builder).withConfig(mAccount);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        builder.withTabColor(tabColor);
        verify(builder).withTabColor(tabColor);

        builder.supportedBrowsers(supportBrowsers);
        verify(builder).supportedBrowsers(supportBrowsers);

        Object client = builder.create();
        verify(builder).create();
        assertEquals(otherClient, client);
    }

    @Test
    public void testSyncNativeBuilder() {
        Okta.SyncNativeBuilder builder = mock(Okta.SyncNativeBuilder.class);
        SyncAuthClient otherClient = new Okta.SyncNativeBuilder()
                .withConfig(mAccount)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .create();
        when(builder.create()).thenReturn(otherClient);

        builder.withConfig(mAccount);
        verify(builder).withConfig(mAccount);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        Object client = builder.create();
        verify(builder).create();
        assertEquals(otherClient, client);
    }

    @Test
    public void testBuilder() {
        Okta.Builder builder = mock(Okta.Builder.class);
        SyncAuthClient otherClient = new Okta.Builder<SyncAuthClient>()
                .withConfig(mAccount)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .withAuthenticationClientFactory(new SyncAuthClientFactoryImpl())
                .create();

        when(builder.create()).thenReturn(otherClient);

        builder.withConfig(mAccount);
        verify(builder).withConfig(mAccount);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        Object client = builder.create();
        verify(builder).create();
        assertEquals(otherClient, client);
    }
}
