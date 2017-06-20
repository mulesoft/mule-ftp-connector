/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static java.lang.String.format;
import static org.apache.commons.net.ftp.FTPFile.DIRECTORY_TYPE;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.command.FileCommand;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.ftp.ClassicFtpFileAttributes;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;
import java.util.Stack;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Base class for implementations of {@link FileCommand} which operate on a FTP server
 *
 * @since 1.0
 */
abstract class ClassicFtpCommand extends FtpCommand<ClassicFtpFileSystem> {

  protected final FTPClient client;

  /**
   * Creates a new instance
   *
   * @param fileSystem the {@link FileSystem} on which the operation is performed
   * @param client a ready to use {@link FTPClient} to perform the operations
   */
  ClassicFtpCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem);
    this.client = client;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected FtpFileAttributes getFile(String filePath, boolean requireExistence) {
    Path path = resolvePath(filePath);
    Optional<FTPFile> ftpFile;
    try {
      ftpFile = getFileFromPath(path);
    } catch (Exception e) {
      throw exception("Found exception trying to obtain path " + path, e);
    }

    if (ftpFile.isPresent()) {
      return new ClassicFtpFileAttributes(path, ftpFile.get());
    } else {
      if (requireExistence) {
        throw pathNotFoundException(path);
      } else {
        return null;
      }
    }
  }

  /**
   * Creates the directory pointed by {@code directoryPath} also creating any missing parent directories
   *
   * @param directoryPath the {@link Path} to the directory you want to create
   */
  @Override
  protected void doMkDirs(Path directoryPath) {
    String cwd = getCurrentWorkingDirectory();
    Stack<Path> fragments = new Stack<>();
    try {
      for (int i = directoryPath.getNameCount(); i > 0; i--) {
        Path subPath = Paths.get("/").resolve(directoryPath.subpath(0, i));
        if (tryChangeWorkingDirectory(subPath.toString())) {
          break;
        }
        fragments.push(subPath);
      }

      while (!fragments.isEmpty()) {
        Path fragment = fragments.pop();
        makeDirectory(fragment.toString());
        changeWorkingDirectory(fragment);
      }
    } catch (Exception e) {
      throw exception("Found exception trying to recursively create directory " + directoryPath, e);
    } finally {
      changeWorkingDirectory(cwd);
    }
  }

  /**
   * Attempts to change the current working directory of the FTP {@link #client}. If it was not possible (for example, because it
   * doesn't exists), it returns {@code false}
   *
   * @param path the path to which you wish to move
   * @return {@code true} if the CWD was changed. {@code false} otherwise
   */
  @Override
  protected boolean tryChangeWorkingDirectory(String path) {
    try {
      return client.changeWorkingDirectory(path);
    } catch (IOException e) {
      throw exception("Exception was found while trying to change working directory to " + path, e);
    }
  }

  /**
   * Creates the directory of the given {@code directoryName} in the current working directory
   *
   * @param directoryName the name of the directory you want to create
   */
  protected void makeDirectory(String directoryName) {
    try {
      if (!client.makeDirectory(directoryName)) {
        throw exception("Failed to create directory " + directoryName);
      }
    } catch (Exception e) {
      throw exception("Exception was found trying to create directory " + directoryName, e);
    }
  }

  /**
   * @return the {@link #client}'s working directory
   */
  @Override
  protected String getCurrentWorkingDirectory() {
    try {
      return client.printWorkingDirectory();
    } catch (Exception e) {
      throw exception("Failed to determine current working directory");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doRename(String filePath, String newName) throws Exception {
    boolean result = client.rename(filePath, newName);
    if (!result) {
      throw new MuleRuntimeException(createStaticMessage(format("Could not rename path '%s' to '%s'", filePath, newName)));
    }
  }


  /**
   * {@inheritDoc} Same as the super method but adding the FTP rely code
   */
  @Override
  public RuntimeException exception(String message) {
    return super.exception(enrichExceptionMessage(message));
  }

  /**
   * {@inheritDoc} Same as the super method but adding the FTP rely code
   */
  @Override
  public RuntimeException exception(String message, Exception cause) {
    if (cause instanceof FTPConnectionClosedException) {
      cause = new ConnectionException(cause);
    }
    return super.exception(enrichExceptionMessage(message), cause);
  }

  private String enrichExceptionMessage(String message) {
    return format("%s. Ftp reply code: %d", message, client.getReplyCode());
  }

  private Optional<FTPFile> getFileFromPath(Path path) throws IOException {
    String filePath = path.toAbsolutePath().toString();
    // Check if MLST command is supported
    FTPFile file = client.mlistFile(filePath);
    if (file == null) {
      FTPFile[] files = client.listFiles(filePath);
      if (files.length >= 1) {
        if (filePath.endsWith(files[0].getName())) {
          // List command result is the file from the path parameter
          file = files[0];
        } else {
          // List command result is a directory
          if (path.getParent() != null) {
            files = client.listDirectories(path.getParent().toAbsolutePath().toString());
            return Arrays.stream(files).filter(dir -> filePath.endsWith(dir.getName())).findFirst();
          } else {
            // root directory
            file = createRootFile();
          }
        }
      } else if (files.length == 0) {
        file = null;
      }
    }

    return Optional.ofNullable(file);
  }

  /**
   * @return an {@link FTPFile} that represents the root directory of the ftp server
   */
  private FTPFile createRootFile() {
    FTPFile file = new FTPFile();
    file.setName(ROOT);
    file.setType(DIRECTORY_TYPE);
    file.setTimestamp(Calendar.getInstance());
    return file;
  }
}
