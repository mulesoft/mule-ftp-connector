/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static java.lang.String.format;
import static org.mule.extension.ftp.internal.ftp.FtpUtils.normalizePath;

import org.apache.commons.io.FilenameUtils;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.command.WriteCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ClassicFtpCommand} which implements the {@link WriteCommand} contract
 *
 * @since 1.0
 */
public final class FtpWriteCommand extends ClassicFtpCommand implements WriteCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpWriteCommand.class);

  /**
   * {@inheritDoc}
   */
  public FtpWriteCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectory,
                    String encoding) {
    Path path = resolvePath(filePath);
    FileAttributes file = getFile(filePath);

    if (file == null) {
      assureParentFolderExists(path, createParentDirectory);
    } else {
      if (mode == FileWriteMode.CREATE_NEW) {
        throw new FileAlreadyExistsException(format(
                                                    "Cannot write to path '%s' because it already exists and write mode '%s' was selected. "
                                                        + "Use a different write mode or point to a path which doesn't exists",
                                                    path, mode));
      } else if (mode == FileWriteMode.OVERWRITE) {
        fileSystem.delete(file.getPath());
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
  }

  private OutputStream getOutputStream(String path, FileWriteMode mode) {
    try {
      return mode == FileWriteMode.APPEND ? client.appendFileStream(path) : client.storeFileStream(path);
    } catch (Exception e) {
      throw exception(format("Could not open stream to write to path '%s' using mode '%s'", path, mode), e);
    }
  }
}
