/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.ftp.api.UriUtils.createUri;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.ftp.internal.operation.DeleteCommand;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import java.io.IOException;
import java.net.URI;

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
    FtpFileAttributes ftpFileAttributes = getExistingFile(filePath);
    boolean isDirectory = ftpFileAttributes.isDirectory();
    URI uri = createUri(ftpFileAttributes.getPath());

    if (isDirectory) {
      LOGGER.debug("Preparing to delete directory '{}'", uri.getPath());
      deleteDirectory(uri);
    } else {
      deleteFile(uri);
    }
  }

  private void deleteFile(URI uri) {
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
    changeWorkingDirectory(uri.getPath());
    FTPFile[] files;
    try {
      files = client.listFiles();
    } catch (IOException e) {
      throw exception(format("Could not list contents of directory '%s' while trying to delete it", uri.getPath()), e);
    }

    for (FTPFile file : files) {
      if (file != null && isVirtualDirectory(file.getName())) {
        continue;
      }

      FtpFileAttributes ftpFileAttributes = new FtpFileAttributes(createUri(uri.getPath(), file.getName()), file);

      final URI fileUri = createUri(ftpFileAttributes.getPath());
      if (ftpFileAttributes.isDirectory()) {
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
