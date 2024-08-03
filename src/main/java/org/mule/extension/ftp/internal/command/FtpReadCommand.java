/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.ftp.internal.util.UriUtils.createUri;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.operation.ReadCommand;
import org.mule.extension.ftp.internal.lock.NullUriLock;
import org.mule.extension.ftp.internal.lock.UriLock;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.ClassicFtpInputStream;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;

import java.net.URI;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link FtpCommand} which implements the {@link FtpReadCommand}
 *
 * @since 1.0
 */
public final class FtpReadCommand extends FtpCommand implements ReadCommand {

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
    return read(config, filePath, lock, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, String filePath, boolean lock,
                                                     Long timeBetweenSizeCheck) {
    FtpFileAttributes attributes = getExistingFile(filePath);
    if (attributes.isDirectory()) {
      throw cannotReadDirectoryException(createUri(attributes.getPath()));
    }

    return read(config, attributes, lock, timeBetweenSizeCheck, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, FtpFileAttributes attributes, boolean lock,
                                                     Long timeBetweenSizeCheck) {
    return read(config, attributes, lock, timeBetweenSizeCheck, false);
  }

  private Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, FtpFileAttributes attributes, boolean lock,
                                                      Long timeBetweenSizeCheck, boolean useCurrentConnection) {
    URI uri = createUri(attributes.getPath());
    UriLock uriLock = lock ? fileSystem.lock(uri) : new NullUriLock(uri);

    InputStream payload = null;
    try {
      payload = getFileInputStream((FtpConnector) config, attributes, uriLock, timeBetweenSizeCheck, useCurrentConnection);
      return Result.<InputStream, FtpFileAttributes>builder().output(payload)
          .mediaType(fileSystem.getFileMessageMediaType(attributes))
          .attributes(attributes).build();
    } catch (Exception e) {
      uriLock.release();
      closeQuietly(payload);
      throw exception(format("Could not fetch file '%s'. %s", uri.getPath(), e.getMessage()), e);
    }
  }

  private InputStream getFileInputStream(FtpConnector config, FtpFileAttributes attributes, UriLock uriLock,
                                         Long timeBetweenSizeCheck, boolean useCurrentConnection)
      throws ConnectionException {
    if (useCurrentConnection) {
      return ClassicFtpInputStream.newInstance(fileSystem, attributes, uriLock, timeBetweenSizeCheck);
    } else {
      return ClassicFtpInputStream.newInstance(config, attributes, uriLock, timeBetweenSizeCheck);
    }
  }

}
