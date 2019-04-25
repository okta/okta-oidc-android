package com.okta.oidc;

import android.content.Context;
import android.graphics.Color;
import android.provider.CalendarContract;

import com.okta.oidc.clients.AsyncNativeAuth;
import com.okta.oidc.clients.AsyncWebAuth;
import com.okta.oidc.clients.SyncNativeAuth;
import com.okta.oidc.clients.SyncNativeAuthClientFactory;
import com.okta.oidc.clients.SyncWebAuth;
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

import androidx.test.platform.app.InstrumentationRegistry;

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
        AsyncWebAuth otherClient = new Okta.AsyncWebBuilder()
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
        AsyncNativeAuth otherClient = new Okta.AsyncNativeBuilder()
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
        SyncWebAuth otherClient = new Okta.SyncWebBuilder()
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
        SyncNativeAuth otherClient = new Okta.SyncNativeBuilder()
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
        SyncNativeAuth otherClient = new Okta.Builder<SyncNativeAuth>()
                .withConfig(mAccount)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .withAuthenticationClientFactory(new SyncNativeAuthClientFactory())
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
