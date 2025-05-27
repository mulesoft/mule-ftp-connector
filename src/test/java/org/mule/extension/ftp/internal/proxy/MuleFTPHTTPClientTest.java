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


  @Mock
  private ProxySettings proxySettings;

  private MuleFTPHTTPClient client;

  @Before
  public void setUp() {
    when(proxySettings.getHost()).thenReturn("test.host.com");
    when(proxySettings.getPort()).thenReturn(8080);
    when(proxySettings.getUsername()).thenReturn("testUser");
    when(proxySettings.getPassword()).thenReturn("testPass");
  }

  @Test
  public void testConstructorWithBasicProxy() throws Exception {
    client = new MuleFTPHTTPClient(proxySettings);
    assert client != null;
  }

  @Test(expected = IOException.class)
  public void testConnectWithInvalidHost() throws Exception {
    client = new MuleFTPHTTPClient(proxySettings);
    client.connect("invalid.host.that.does.not.exist", -1);
  }

}
