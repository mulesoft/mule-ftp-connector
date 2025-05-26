/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.proxy;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpTunnelProxyTest {

  @Test
  public void testGettersAndSetters() {
    HttpTunnelProxy proxy = new HttpTunnelProxy();

    proxy.host = "test.host.com";
    proxy.port = 8080;
    proxy.username = "testuser";
    proxy.password = "testpass";

    assertEquals("test.host.com", proxy.getHost());
    assertEquals(8080, proxy.getPort());
    assertEquals("testuser", proxy.getUsername());
    assertEquals("testpass", proxy.getPassword());
  }

  @Test
  public void testEqualsAndHashCode() {
    HttpTunnelProxy proxy1 = new HttpTunnelProxy();
    HttpTunnelProxy proxy2 = new HttpTunnelProxy();

    assertTrue(proxy1.equals(proxy1));
    assertTrue(proxy1.equals(proxy2));
    assertEquals(proxy1.hashCode(), proxy2.hashCode());

    proxy1.host = "host1";
    proxy1.port = 8080;
    proxy1.username = "user1";
    proxy1.password = "pass1";

    proxy2.host = "host2";
    proxy2.port = 8081;
    proxy2.username = "user2";
    proxy2.password = "pass2";

    assertFalse(proxy1.equals(proxy2));
    assertNotEquals(proxy1.hashCode(), proxy2.hashCode());

    assertFalse(proxy1.equals(null));
    assertFalse(proxy1.equals("Not a proxy"));
  }

  @Test
  public void testEqualsWithDifferentFields() {
    HttpTunnelProxy proxy1 = new HttpTunnelProxy();
    HttpTunnelProxy proxy2 = new HttpTunnelProxy();

    proxy1.host = "host1";
    proxy2.host = "host2";
    assertFalse(proxy1.equals(proxy2));
    proxy2.host = "host1";
    assertTrue(proxy1.equals(proxy2));
  }
}
