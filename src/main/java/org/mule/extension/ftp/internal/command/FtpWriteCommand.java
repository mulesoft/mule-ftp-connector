/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.ftp.api.FileWriteMode.APPEND;
import static org.mule.extension.ftp.api.FileWriteMode.CREATE_NEW;
import static org.mule.extension.ftp.api.FileWriteMode.OVERWRITE;
import static org.mule.extension.ftp.internal.util.UriUtils.createUri;
import static org.mule.extension.ftp.internal.util.UriUtils.trimLastFragment;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.api.FileWriteMode;
import org.mule.extension.ftp.internal.operation.WriteCommand;
import org.mule.extension.ftp.internal.exception.FileAlreadyExistsException;
import org.mule.extension.ftp.internal.exception.IllegalPathException;
import org.mule.extension.ftp.internal.lock.NullUriLock;
import org.mule.extension.ftp.internal.lock.UriLock;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
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
  @Deprecated
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
    URI baseUri = createUri("/", fileSystem.getBasePath());
    URI uri = createUri(baseUri.getPath(), filePath);
    UriLock uriLock = lock ? fileSystem.lock(uri) : new NullUriLock(uri);
    String normalizedPath = normalizePath(uri.getPath());
    OutputStream outputStream = null;
    boolean outputStreamObtained = false;
    try {
      if (mode != CREATE_NEW && canWriteToPathDirectly(uri)) {
        try {
          outputStream = getOutputStream(normalizedPath, mode);
          if (FTPReply.isPositivePreliminary(client.getReplyCode())) {
            outputStreamObtained = true;
          } else {
            closeSilently(outputStream);
            outputStream = null;
          }
        } catch (Exception e) {
          // Something went wrong while trying to get the OutputStream to write the file, do not fail here and do a full
          // validation.
        }
      }

      if (!outputStreamObtained) {
        validateUri(uri, createParentDirectory, mode);
      }

      try {
        if (!outputStreamObtained) {
          outputStream = getOutputStream(normalizedPath, mode);
        }
        IOUtils.copy(content, outputStream);
        LOGGER.debug("Successfully wrote to path {}", normalizedPath);
      } catch (Exception e) {
        throw exception(format("Exception was found writing to file '%s'", normalizedPath), e);
      } finally {
        closeSilently(outputStream);
        fileSystem.awaitCommandCompletion();
      }
    } finally {
      uriLock.release();
    }
  }

  private void validateUri(URI uri, boolean createParentDirectory, FileWriteMode mode) {
    FtpFileAttributes file = getFile(uri.getPath(), false);
    if (file == null) {
      if (pathIsDirectory(uri)) {
        throw pathIsADirectoryException(uri);
      }
      FtpFileAttributes directory = getFile(trimLastFragment(uri).getPath(), false);
      if (directory == null) {
        assureParentFolderExists(uri, createParentDirectory);
      }
    } else {
      if (mode == CREATE_NEW) {
        throw new FileAlreadyExistsException(format(
                                                    "Cannot write to path '%s' because it already exists and write mode '%s' was selected. "
                                                        + "Use a different write mode or point to a path which doesn't exist",
                                                    uri.getPath(), mode));
      } else if (mode == OVERWRITE) {
        if (file.isDirectory()) {
          throw pathIsADirectoryException(uri);
        }
      }
    }
  }

  private IllegalPathException pathIsADirectoryException(URI uri) {
    return new IllegalPathException(String.format("Cannot write file to path '%s' because it is a directory",
                                                  uri.getPath()));
  }

  private boolean canWriteToPathDirectly(URI uri) {
    return parentDirectoryExists(uri) && !pathIsDirectory(uri);
  }

  private boolean parentDirectoryExists(URI uri) {
    return getUriToDirectory(trimLastFragment(uri).getPath()).isPresent();
  }

  private boolean pathIsDirectory(URI uri) {
    return getUriToDirectory(uri.getPath()).isPresent();
  }

  private void closeSilently(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (Exception e) {
      // Do nothing
    }
  }

  /**
   * Method to remove '/' when the file path is under root directory only.
   * ex. '/abc.txt' 
   * @param path, normalized path 
   * @return updated path
   */
  private String removeSlashUnderRootDirOnly(String path) {
    if (path != null && path.length() > 1 && path.charAt(0) == SEPARATOR.charAt(0)
        && StringUtils.countMatches(path, SEPARATOR) == 1) {
      return path.substring(1);
    }
    return path;
  }

  private OutputStream getOutputStream(String path, FileWriteMode mode) {
    try {
      path = removeSlashUnderRootDirOnly(path);
      return mode == APPEND ? client.appendFileStream(path) : client.storeFileStream(path);
    } catch (Exception e) {
      throw exception(format("Could not open stream to write to path '%s' using mode '%s'", path, mode), e);
    }
  }
}
