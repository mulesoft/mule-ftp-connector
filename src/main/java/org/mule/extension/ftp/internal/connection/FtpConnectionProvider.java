/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static java.lang.String.format;

import org.mule.extension.ftp.api.proxy.ProxySettings;
import org.mule.extension.ftp.internal.proxy.MuleFTPHTTPClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects to an FTP server
 *
 * @since 1.0
 */
@DisplayName("FTP Connection")
@Summary("Connection to connect against an FTP server")
public class FtpConnectionProvider extends FtpAbstractConnectionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpConnectionProvider.class);

  @Parameter
  @Optional
  @Summary("Enables you to set HTTP or HTTPS tunnel proxy.")
  protected ProxySettings proxy;

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(proxy);
  }

  protected FTPClient createClient() {
    FTPClient client;
    if (proxy == null)
      client = new FTPClient();
    else {
      try {
        client = new MuleFTPHTTPClient(proxy);
        client.setUseEPSVwithIPv4(true);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not create FTP client"), e);
      }
      LOGGER.debug(format("Connecting to proxy host: '%s' at port: '%d'", proxy.getHost(),
                          proxy.getPort()));
    }

    if (LOGGER.isDebugEnabled()) {
      setupWireLogging(client, LOGGER::debug);
    }
    return client;
  }

}
