package com.okta.oidc;

import android.content.Context;

import com.google.gson.Gson;
import com.okta.oidc.deprecated.AuthenticateClient;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpConnectionFactory;
import com.okta.oidc.storage.OktaStorage;
import com.okta.oidc.storage.SimpleOktaStorage;
import com.okta.oidc.util.JsonStrings;
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

    private OIDCAccount mAccount;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private HttpConnectionFactory mConnectionFactory;
    private OktaStorage mStorage;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mStorage = new SimpleOktaStorage(mContext);
        mConnectionFactory = new HttpConnection.DefaultConnectionFactory();

        String url = new MockEndPoint().getUrl();
        mAccount = TestValues.getAccountWithUrl(url);
    }

    @Test
    public void testBuilder() {
        AuthenticateClient.Builder builder = mock(AuthenticateClient.Builder.class);
        AuthenticateClient otherClient = new AuthenticateClient.Builder()
                .withAccount(mAccount)
                .withTabColor(0)
                .withCallbackExecutor(mExecutor)
                .withStorage(mStorage)
                .withContext(mContext)
                .withHttpConnectionFactory(mConnectionFactory)
                .create();

        when(builder.create()).thenReturn(otherClient);

        builder.withAccount(mAccount);
        verify(builder).withAccount(mAccount);

        builder.withTabColor(0);
        verify(builder).withTabColor(0);

        builder.withStorage(mStorage);
        verify(builder).withStorage(mStorage);

        builder.withHttpConnectionFactory(mConnectionFactory);
        verify(builder).withHttpConnectionFactory(mConnectionFactory);

        builder.withCallbackExecutor(mExecutor);
        verify(builder).withCallbackExecutor(mExecutor);

        builder.withContext(mContext);
        verify(builder).withContext(mContext);

        builder.supportedBrowsers(JsonStrings.FIRE_FOX);
        verify(builder).supportedBrowsers(JsonStrings.FIRE_FOX);

        AuthenticateClient client = builder.create();
        verify(builder).create();
        assertEquals(otherClient, client);
    }
}
