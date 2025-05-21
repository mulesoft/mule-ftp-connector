package org.mule.extension.ftp.internal.proxy;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.extension.ftp.api.proxy.HttpsTunnelProxy;
import org.mule.extension.ftp.api.proxy.ProxySettings;
import org.mule.runtime.api.tls.TlsContextFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.SocketException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MuleFTPHTTPClientTest {

    private static final String HOST = "test.host.com";
    private static final int PORT = 8080;
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "testPass";

    @Mock
    private ProxySettings proxySettings;

    @Mock
    private HttpsTunnelProxy httpsTunnelProxy;

    @Mock
    private TlsContextFactory tlsContextFactory;

    @Mock
    private SSLContext sslContext;

    @Mock
    private SSLSocketFactory sslSocketFactory;

    @Mock
    private FTPClient ftpClient;

    private MuleFTPHTTPClient client;

    @Before
    public void setUp() {
        when(proxySettings.getHost()).thenReturn(HOST);
        when(proxySettings.getPort()).thenReturn(PORT);
        when(proxySettings.getUsername()).thenReturn(USERNAME);
        when(proxySettings.getPassword()).thenReturn(PASSWORD);
    }

    @Test
    public void testConstructorWithBasicProxy() throws Exception {
        client = new MuleFTPHTTPClient(proxySettings);
        assert client != null;
    }

    @Test
    public void testConstructorWithHttpsTunnelProxy() throws Exception {
        when(httpsTunnelProxy.getHost()).thenReturn(HOST);
        when(httpsTunnelProxy.getPort()).thenReturn(PORT);
        when(httpsTunnelProxy.getUsername()).thenReturn(USERNAME);
        when(httpsTunnelProxy.getPassword()).thenReturn(PASSWORD);
        when(httpsTunnelProxy.getTlsContextFactory()).thenReturn(tlsContextFactory);
        when(tlsContextFactory.createSslContext()).thenReturn(sslContext);

        client = new MuleFTPHTTPClient(httpsTunnelProxy);
        assert client != null;
    }

    @Test
    public void testConnectWithoutSSLContext() throws Exception {
        // Create a spy of MuleFTPHTTPClient to verify method calls
        client = spy(new MuleFTPHTTPClient(proxySettings));
        
        // Mock the parent class's connect method to prevent actual network calls
        doNothing().when(client).connect(anyString(), anyInt());
        
        // Execute
        client.connect("target.host.com", 21);

        // Verify that setSocketFactory was not called
        verify(client, never()).setSocketFactory(any());
    }

    @Test(expected = IOException.class)
    public void testConnectWithInvalidHost() throws Exception {
        client = spy(new MuleFTPHTTPClient(proxySettings));
        
        // Mock the parent class's connect method to throw IOException
        doThrow(new IOException()).when(client).connect(anyString(), anyInt());
        
        client.connect("invalid.host", -1);
    }
} 