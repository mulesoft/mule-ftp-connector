/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.proxy;

import org.mule.extension.ftp.api.proxy.HttpsTunnelProxy;
import org.mule.extension.ftp.api.proxy.ProxySettings;
import org.mule.runtime.api.tls.TlsContextFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.SocketException;
import org.apache.commons.net.ftp.FTPHTTPClient;

/**
 * Extends the org.apache.commons.net.ftp.FTPHTTPClient for added functionality for HTTPs
 *
 * @since 1.6.0
 */
public class MuleFTPHTTPClient extends FTPHTTPClient {

  protected ProxySettings proxy;
  protected SSLContext context;

  public MuleFTPHTTPClient(ProxySettings proxy) throws Exception {
    super(proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());
    this.proxy = proxy;
    if (proxy instanceof HttpsTunnelProxy) {
      TlsContextFactory tlsContextFactory = ((HttpsTunnelProxy) proxy).getTlsContextFactory();
      if (tlsContextFactory != null) {
        this.context = tlsContextFactory.createSslContext();
      }
    }
  }

  @Override
  public void connect(String host, int port) throws SocketException, IOException {
    if (context != null) {
      setSocketFactory(context.getSocketFactory());
    }
    super.connect(host, port);
  }

}
