/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import org.mule.extension.file.common.api.command.CreateDirectoryCommand;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link CreateDirectoryCommand}
 *
 * @since 1.0
 */
public final class FtpCreateDirectoryCommand extends FtpCommand implements CreateDirectoryCommand {

  /**
   * {@inheritDoc}
   */
  public FtpCreateDirectoryCommand(FtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createDirectory(String directoryPath) {
    super.createDirectory(directoryPath);
  }
}
