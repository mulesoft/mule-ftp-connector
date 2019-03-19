/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.file.common.api.FileWriteMode.APPEND;
import static org.mule.extension.file.common.api.FileWriteMode.CREATE_NEW;
import static org.mule.extension.file.common.api.FileWriteMode.OVERWRITE;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.command.WriteCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.file.common.api.lock.NullPathLock;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FtpCommand} which implements the {@link WriteCommand} contract
 *
 * @since 1.0
 */
public final class FtpWriteCommand extends FtpCommand implements WriteCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpWriteCommand.class);

  /**
   * {@inheritDoc}
   */
  public FtpWriteCommand(FtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectory,
                    String encoding) {
    write(filePath, content, mode, lock, createParentDirectory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectory) {
    Path path = resolvePathFromBasePath(filePath);
    FileAttributes file = getFile(path, false);

    PathLock pathLock = lock ? fileSystem.lock(path) : new NullPathLock(path);
    try {

      if (file == null) {
        FileAttributes directory = getFile(path.getParent(), false);
        if (directory == null) {
          assureParentFolderExists(path, createParentDirectory);
        }
      } else {
        if (mode == CREATE_NEW) {
          throw new FileAlreadyExistsException(format(
                                                      "Cannot write to path '%s' because it already exists and write mode '%s' was selected. "
                                                          + "Use a different write mode or point to a path which doesn't exist",
                                                      path, mode));
        } else if (mode == OVERWRITE) {
          if (file.isDirectory()) {
            throw new IllegalPathException(String.format("Cannot write file to path '%s' because it is a directory",
                                                         file.getPath()));
          }
        }
      }

      String normalizedPath = normalizePath(path);
      try (OutputStream outputStream = getOutputStream(normalizedPath, mode)) {
        IOUtils.copy(content, outputStream);
        LOGGER.debug("Successfully wrote to path {}", normalizedPath);
      } catch (Exception e) {
        throw exception(format("Exception was found writing to file '%s'", normalizedPath), e);
      } finally {
        fileSystem.awaitCommandCompletion();
      }
    } finally {
      pathLock.release();
    }
  }

  private OutputStream getOutputStream(String path, FileWriteMode mode) {
    try {
      return mode == APPEND ? client.appendFileStream(path) : client.storeFileStream(path);
    } catch (Exception e) {
      throw exception(format("Could not open stream to write to path '%s' using mode '%s'", path, mode), e);
    }
  }
}
