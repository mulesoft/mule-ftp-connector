/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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
    
    // Verify the SSL context was properly set
    verify(httpsTunnelProxy).getTlsContextFactory();
    verify(tlsContextFactory).createSslContext();
    assert client != null;
    // Access the protected context field through reflection to verify it was set
    java.lang.reflect.Field contextField = MuleFTPHTTPClient.class.getDeclaredField("context");
    contextField.setAccessible(true);
    SSLContext actualContext = (SSLContext) contextField.get(client);
    assert actualContext == sslContext;
  }

  @Test(expected = IOException.class)
  public void testConnectWithInvalidHost() throws Exception {
    // Create client with proxy settings
    client = new MuleFTPHTTPClient(proxySettings);
    
    // Try to connect to a non-existent host with invalid port
    // This should naturally throw IOException due to connection failure
    client.connect("invalid.host.that.does.not.exist", -1);
  }

  
}
