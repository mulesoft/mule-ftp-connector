/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.command.DeleteCommand;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

public final class FtpDeleteCommand extends FtpCommand implements DeleteCommand {

  private static Logger LOGGER = getLogger(FtpDeleteCommand.class);

  /**
   * {@inheritDoc}
   */
  public FtpDeleteCommand(FtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String filePath) {
    FileAttributes fileAttributes = getExistingFile(filePath);
    boolean isDirectory = fileAttributes.isDirectory();
    Path path = Paths.get(fileAttributes.getPath());
    URI uri = createUri(fileAttributes.getPath());

    if (isDirectory) {
      LOGGER.debug("Preparing to delete directory '{}'", path);
      deleteDirectory(uri);
    } else {
      deleteFile(uri);
    }
  }

  private void deleteFile(URI uri) {
    Path filePath = Paths.get(uri.getPath());
    fileSystem.verifyNotLocked(uri);
    try {
      if (!client.deleteFile(normalizePath(uri.getPath()))) {
        throw exception("Could not delete file " + uri.getPath());
      }
    } catch (Exception e) {
      throw exception("Found Exception while deleting directory " + uri.getPath(), e);
    }
    logDelete(uri);
  }

  private void deleteDirectory(URI uri) {
    Path path = Paths.get(uri.getPath());
    changeWorkingDirectory(path);
    changeWorkingDirectory(uri.getPath());
    FTPFile[] files;
    try {
      files = client.listFiles();
    } catch (IOException e) {
      throw exception(format("Could not list contents of directory '%s' while trying to delete it", uri.getPath()), e);
    }

    for (FTPFile file : files) {
      if (isVirtualDirectory(file.getName())) {
        continue;
      }

      FileAttributes fileAttributes2 = new FtpFileAttributes(path.resolve(file.getName()), file);
      FileAttributes fileAttributes = new FtpFileAttributes(createUri(uri.getPath(), file.getName()), file);

      final Path filePath = Paths.get(fileAttributes.getPath());
      final URI fileUri = createUri(fileAttributes.getPath());
      if (fileAttributes.isDirectory()) {
        deleteDirectory(fileUri);
      } else {
        deleteFile(fileUri);
      }
    }

    boolean removed;
    try {
      client.changeToParentDirectory();
      removed = client.removeDirectory(uri.getPath());
    } catch (IOException e) {
      throw exception("Found exception while trying to remove directory " + uri.getPath(), e);
    }

    if (!removed) {
      throw exception("Could not remove directory " + uri.getPath());
    }

    logDelete(uri);
  }

  private void logDelete(URI uri) {
    LOGGER.debug("Successfully deleted '{}'", uri.getPath());
  }
}
