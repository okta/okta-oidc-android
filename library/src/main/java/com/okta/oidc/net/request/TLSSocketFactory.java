/*
 * Copyright (c) 2018, Okta, Inc. and/or its affiliates. All rights reserved.
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

import android.support.annotation.RestrictTo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * SSLSocketFactory which wraps default SSLSocketFactory and enable TLS v1.1, v1.2.
 */
@RestrictTo(LIBRARY_GROUP)
public class TLSSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory mInternalSslSocketFactory;
    private String[] mProtocolsToEnable = {"TLSv1.1", "TLSv1.2"};

    /**
     * Constructs an TlsEnableSocketFactory object.
     *
     * @throws KeyManagementException if init operation fails
     * @throws NoSuchAlgorithmException when get SSLContext
     */
    public TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        mInternalSslSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mInternalSslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mInternalSslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTlsOnSocket(mInternalSslSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        return enableTlsOnSocket(mInternalSslSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTlsOnSocket(mInternalSslSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        return enableTlsOnSocket(mInternalSslSocketFactory
                .createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTlsOnSocket(mInternalSslSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(
            InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return enableTlsOnSocket(mInternalSslSocketFactory
                .createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTlsOnSocket(Socket socket) {
        if ( socket != null && (socket instanceof SSLSocket) ) {
            ((SSLSocket)socket).setEnabledProtocols(mProtocolsToEnable);
        }
        return socket;
    }
}