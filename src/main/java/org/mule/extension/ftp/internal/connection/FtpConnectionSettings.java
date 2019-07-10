/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Groups FTP connection parameters
 *
 * @since 1.0
 */
public final class FtpConnectionSettings {

  /**
   * The FTP server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
   */
  @Parameter
  @Placement(order = 1)
  private String host;

  /**
   * The port number of the FTP server to connect
   */
  @Parameter
  @Optional(defaultValue = "21")
  @Placement(order = 2)
  private int port = 21;

  /**
   * Username for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String username;

  /**
   * Password for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Password
  @Optional
  @Placement(order = 4)
  private String password;


  /**
   * Enable or disable verification that the remote host taking part
   * of a data connection is the same as the host to which the control
   * connection is attached.  The default is for verification to be
   * enabled.  You may set this value at any time, whether the
   * FTPClient is currently connected or not.
   */
  @Parameter
  @Optional
  @Placement(order = 5)
  private boolean remoteVerificationEnabled = true;


  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setRemoteVerificationEnabled(boolean remoteVerificationEnabled) {
      this.remoteVerificationEnabled = remoteVerificationEnabled;
  }

  public boolean isRemoteVerificationEnabled() {
      return this.remoteVerificationEnabled;
  }

}
