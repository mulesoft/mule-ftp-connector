/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ReadCommand;
import org.mule.extension.file.common.api.lock.NullPathLock;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.ClassicFtpInputStream;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link FtpReadCommand}
 *
 * @since 1.0
 */
public final class FtpReadCommand extends FtpCommand implements ReadCommand<FtpFileAttributes> {

  /**
   * {@inheritDoc}
   */
  public FtpReadCommand(FtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, String filePath, boolean lock) {
    FtpFileAttributes attributes = getExistingFile(filePath);
    if (attributes.isDirectory()) {
      throw cannotReadDirectoryException(Paths.get(attributes.getPath()));
    }


    return read(config, attributes, lock);
  }

  public Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, FtpFileAttributes attributes, boolean lock) {
    Path path = Paths.get(attributes.getPath());
    PathLock pathLock = lock ? fileSystem.lock(path) : new NullPathLock(path);

    InputStream payload = null;
    try {
      payload = ClassicFtpInputStream.newInstance((FtpConnector) config, attributes, pathLock);
      return Result.<InputStream, FtpFileAttributes>builder().output(payload)
          .mediaType(fileSystem.getFileMessageMediaType(attributes))
          .attributes(attributes).build();
    } catch (Exception e) {
      pathLock.release();
      closeQuietly(payload);
      throw exception(format("Could not fetch file '%s'. %s", path, e.getMessage()), e);
    }
  }
}
