/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.proxy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;

@RunWith(MockitoJUnitRunner.class)
public class HttpsTunnelProxyTest {

  private HttpsTunnelProxy httpsTunnelProxy;

  @Mock
  private TlsContextFactory tlsContextFactory;

  @Mock
  private TlsContextFactoryBuilder tlsContextFactoryBuilder;

  @Before
  public void setUp() {
    httpsTunnelProxy = new HttpsTunnelProxy();
  }

  @Test
  public void testGetTlsContextFactory() throws Exception {
    setPrivateField(httpsTunnelProxy, "tlsContextFactory", tlsContextFactory);
    assertSame(tlsContextFactory, httpsTunnelProxy.getTlsContextFactory());
  }

  @Test
  public void testInitialiseWithNullTlsContextFactory() throws Exception {
    // When tlsContextFactory is null, it should be initialized with default settings
    httpsTunnelProxy.initialise();
    assertNotNull(httpsTunnelProxy.getTlsContextFactory());
  }

  @Test
  public void testInitialiseWithExistingTlsContextFactory() throws Exception {
    // When tlsContextFactory is already set, it should be initialized
    setPrivateField(httpsTunnelProxy, "tlsContextFactory", tlsContextFactory);
    httpsTunnelProxy.initialise();
    initialiseIfNeeded(tlsContextFactory);
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
