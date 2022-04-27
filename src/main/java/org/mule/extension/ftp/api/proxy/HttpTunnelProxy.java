/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.proxy;

import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.connectivity.ConfiguresProxy;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.security.Username;

import java.util.Objects;

/**
 * Groups FTP Proxy connection parameters
 *
 * @since 1.0
 */

@Summary("The FTP Proxy Settings")
@ConfiguresProxy
public class HttpTunnelProxy implements ProxySettings {

  /**
   * The FTP Proxy server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
   */
  @Parameter
  @DisplayName("Proxy host")
  @Expression(SUPPORTED)
  @Optional(defaultValue = "")
  @Host
  private String host;

  /**
   * The port number of the FTP Proxy server to connect
   */
  @Parameter
  @DisplayName("Proxy port")
  @Expression(SUPPORTED)
  @Optional(defaultValue = "")
  @Port
  private int port = 3128;

  /**
   * Username for the FTP Proxy Server. Required if the Proxy server is authenticated.
   */
  @Parameter
  @DisplayName("Proxy username")
  @Expression(SUPPORTED)
  @Optional(defaultValue = "")
  @Username
  private String username;

  /**
   * Password for the FTP Proxy Server. Required if the Proxy server is authenticated.
   */
  @Parameter
  @DisplayName("Proxy password")
  @Expression(SUPPORTED)
  @Optional(defaultValue = "")
  @Password
  private String password;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public TlsContextFactory getTlsContextFactory() {
    return null;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    HttpTunnelProxy that = (HttpTunnelProxy) o;
    return port == that.port && Objects.equals(host, that.host) && Objects.equals(username, that.username)
        && Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, username, password);
  }

  @Override
  public void initialise() throws InitialisationException {

  }
}
