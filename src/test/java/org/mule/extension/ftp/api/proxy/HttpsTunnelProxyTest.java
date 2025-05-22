/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.proxy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.api.tls.TlsContextFactory;

@RunWith(MockitoJUnitRunner.class)
public class HttpsTunnelProxyTest {

  private HttpsTunnelProxy httpsTunnelProxy;

  @Mock
  private TlsContextFactory tlsContextFactory;

  @Before
  public void setUp() {
    httpsTunnelProxy = new HttpsTunnelProxy();
  }

  @Test
  public void testGetTlsContextFactory() {
    // Set the TLS context factory through the constructor or setter
    httpsTunnelProxy.setTlsContextFactory(tlsContextFactory);
    assertSame(tlsContextFactory, httpsTunnelProxy.getTlsContextFactory());
  }

  @Test
  public void testInitialiseWithNullTlsContextFactory() throws Exception {
    // When tlsContextFactory is null, it should be initialized with default settings
    httpsTunnelProxy.initialise();
    assertNotNull(httpsTunnelProxy.getTlsContextFactory());
  }
}
