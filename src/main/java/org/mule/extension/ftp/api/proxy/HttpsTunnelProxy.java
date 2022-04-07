/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.proxy;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.connectivity.ConfiguresProxy;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;

/**
 * Groups FTP Proxy connection parameters
 *
 * @since 1.0
 */

@Summary("The FTP Proxy Settings")
@ConfiguresProxy
public class HttpsTunnelProxy extends HttpTunnelProxy {

  private TlsContextFactoryBuilder tlsContextFactoryBuilder = TlsContextFactory.builder();

  @Parameter
  @Optional
  @DisplayName("TLS Configuration")
  private TlsContextFactory tlsContextFactory;

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }

  public void setTlsContextFactory(TlsContextFactory tlsContextFactory) {
    this.tlsContextFactory = tlsContextFactory;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (tlsContextFactory == null) {
      tlsContextFactory = tlsContextFactoryBuilder.buildDefault();
    }
    initialiseIfNeeded(tlsContextFactory);
  }

}
