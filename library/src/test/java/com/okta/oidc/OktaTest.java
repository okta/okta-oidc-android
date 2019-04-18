package com.okta.oidc;

import android.content.Context;

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
