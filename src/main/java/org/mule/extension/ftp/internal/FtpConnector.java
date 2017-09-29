/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.ftp.internal.connection.FtpConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.PrivilegedExport;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

import javax.inject.Inject;

/**
 * Connects to an FTP server
 *
 * @since 1.0
 */
@Extension(name = "FTP")
@Operations({FtpOperations.class})
@ConnectionProviders(FtpConnectionProvider.class)
@PrivilegedExport(packages = {"org.mule.extension.ftp.internal", "org.mule.extension.ftp.internal.connection",
    "org.apache.commons.net.ftp"}, artifacts = {"com.mulesoft.connectors:mule-ftps-connector"})
@ErrorTypes(FileError.class)
@Xml(prefix = "ftp")
public class FtpConnector extends FileConnectorConfig {

  @Inject
  private ConnectionManager connectionManager;


  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }
}
