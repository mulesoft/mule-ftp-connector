/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.net.ftp.FTPFile.DIRECTORY_TYPE;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.command.FileCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.FtpCopyDelegate;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link FileCommand} implementations that target a FTP/SFTP server
 *
 * @since 1.0
 */
public abstract class FtpCommand extends FileCommand<FtpFileSystem> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpCommand.class);
  protected static final String ROOT = "/";

  protected final FTPClient client;

  protected FtpCommand(FtpFileSystem fileSystem) {
    this(fileSystem, fileSystem.getClient());
  }

  /**
   * Creates a new instance
   *
   * @param fileSystem the {@link FileSystem} on which the operation is performed
   * @param client a ready to use {@link FTPClient} to perform the operations
   */
  protected FtpCommand(FtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem);
    this.client = client;
  }

  /**
   * Similar to {@link #getFile(String)} but throwing an {@link IllegalArgumentException} if the
   * {@code filePath} doesn't exist
   *
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes}
   * @throws IllegalArgumentException if the {@code filePath} doesn't exist
   */
  protected FtpFileAttributes getExistingFile(String filePath) {
    return getFile(filePath, true);
  }

  /**
   * Obtains a {@link FtpFileAttributes} for the given {@code filePath} by using the {@link FTPClient#mlistFile(String)} FTP
   * command
   *
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes} or {@code null} if it doesn't exist
   */
  public FtpFileAttributes getFile(String filePath) {
    return getFile(filePath, false);
  }

  protected FtpFileAttributes getFile(String filePath, boolean requireExistence) {
    Path path = resolvePath(normalizePath(filePath));
    Optional<FTPFile> ftpFile;
    try {
      ftpFile = getFileFromPath(path);
    } catch (Exception e) {
      throw exception("Found exception trying to obtain path " + path, e);
    }

    if (ftpFile.isPresent()) {
      return new FtpFileAttributes(path, ftpFile.get());
    } else {
      if (requireExistence) {
        throw pathNotFoundException(path);
      } else {
        return null;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean exists(Path path) {
    return getBasePath(fileSystem).equals(path) || ROOT.equals(path.toString()) || getFile(normalizePath(path)) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Path getBasePath(FileSystem fileSystem) {
    return Paths.get(getCurrentWorkingDirectory());
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the {@link Path} to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(Path path) {
    changeWorkingDirectory(normalizePath(path.toString()));
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the path to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(String path) {
    if (!tryChangeWorkingDirectory(path)) {
      throw new IllegalArgumentException(format("Could not change working directory to '%s'. Path doesn't exist or is not a directory",
                                                path.toString()));
    }
    LOGGER.debug("working directory changed to {}", path);
  }

  /**
   * Returns a {@link Path} relative to the {@code basePath} and the given {@code filePath}
   *
   * @param filePath the path to a file or directory
   * @return a relative {@link Path}
   */
  @Override
  protected Path resolvePath(String filePath) {
    Path path = getBasePath(fileSystem);
    if (filePath != null) {
      path = path.resolve(filePath);
    }

    return path;
  }

  /**
   * Attempts to change the current working directory. If it was not possible (for example, because it doesn't exist), it returns
   * {@code false}
   *
   * @param path the path to which you wish to move
   * @return {@code true} if the CWD was changed. {@code false} otherwise
   */
  protected boolean tryChangeWorkingDirectory(String path) {
    try {
      return client.changeWorkingDirectory(normalizePath(path));
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
      if (!client.makeDirectory(normalizePath(directoryName))) {
        throw exception("Failed to create directory " + directoryName);
      }
    } catch (Exception e) {
      throw exception("Exception was found trying to create directory " + directoryName, e);
    }
  }


  /**
   * Renames the file at {@code filePath} to {@code newName}.
   *
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @param overwrite whether to overwrite the target file if it already exists
   */
  protected void rename(String filePath, String newName, boolean overwrite) {
    Path source = resolveExistingPath(filePath);
    Path target = source.getParent().resolve(newName);

    if (exists(target)) {
      if (!overwrite) {
        throw new FileAlreadyExistsException(format("'%s' cannot be renamed because '%s' already exists", source, target));
      }

      try {
        fileSystem.delete(target.toString());
      } catch (Exception e) {
        throw exception(format("Exception was found deleting '%s' as part of renaming '%s'", target, source), e);
      }
    }

    try {
      boolean result = client.rename(normalizePath(source.toString()), normalizePath(target.toString()));
      if (!result) {
        throw new MuleRuntimeException(createStaticMessage(format("Could not rename path '%s' to '%s'", filePath, newName)));
      }
      LOGGER.debug("{} renamed to {}", filePath, newName);
    } catch (Exception e) {
      throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
    }
  }


  protected void createDirectory(String directoryPath) {
    final Path path = Paths.get(fileSystem.getBasePath()).resolve(directoryPath);
    FileAttributes targetFile = getFile(directoryPath);

    if (targetFile != null) {
      throw new FileAlreadyExistsException(format("Directory '%s' already exists", path.toAbsolutePath()));
    }

    mkdirs(path);
  }

  /**
   * Performs the base logic and delegates into
   * {@link FtpCopyDelegate#doCopy(FileConnectorConfig, FileAttributes, Path, boolean)} to perform the actual
   * copying logic
   *  @param config the config that is parameterizing this operation
   * @param source the path to be copied
   * @param target the path to the target destination
   * @param overwrite whether to overwrite existing target paths
   * @param createParentDirectory whether to create the target's parent directory if it doesn't exist
   */
  protected final void copy(FileConnectorConfig config, String source, String target, boolean overwrite,
                            boolean createParentDirectory, String renameTo, FtpCopyDelegate delegate) {
    FileAttributes sourceFile = getExistingFile(source);
    Path targetPath = resolvePath(target);
    FileAttributes targetFile = getFile(targetPath.toString());
    String targetFileName = isBlank(renameTo) ? Paths.get(source).getFileName().toString() : renameTo;

    if (targetFile != null) {
      if (targetFile.isDirectory()) {
        if (sourceFile.isDirectory() && sourceFile.getName().equals(targetFile.getName()) && !overwrite) {
          throw alreadyExistsException(targetPath);
        } else {
          Path sourcePath = resolvePath(targetFileName);
          if (sourcePath.isAbsolute()) {
            targetPath = targetPath.resolve(sourcePath.getName(sourcePath.getNameCount() - 1));
          } else {
            targetPath = targetPath.resolve(targetFileName);
          }
        }
      } else if (!overwrite) {
        throw alreadyExistsException(targetPath);
      }
    } else {
      if (createParentDirectory) {
        mkdirs(targetPath);
        targetPath = targetPath.resolve(targetFileName);
      } else {
        throw pathNotFoundException(targetPath.toAbsolutePath());
      }
    }

    final String cwd = getCurrentWorkingDirectory();
    delegate.doCopy(config, sourceFile, targetPath, overwrite);
    LOGGER.debug("Copied '{}' to '{}'", sourceFile, targetPath);
    changeWorkingDirectory(cwd);
  }

  /**
   * @return the path of the current working directory
   */
  protected String getCurrentWorkingDirectory() {
    try {
      return client.printWorkingDirectory();
    } catch (Exception e) {
      throw exception("Failed to determine current working directory");
    }
  }

  private Optional<FTPFile> getFileFromPath(Path path) throws IOException {
    String filePath = normalizePath(path);
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
          return getDirectoryFromParent(path);
        }
      } else if (files.length == 0) {
        // List command result may be an empty directory
        return getDirectoryFromParent(path);
      }
    }
    return Optional.ofNullable(file);
  }

  private Optional<FTPFile> getDirectoryFromParent(Path path) throws IOException {
    String filePath = normalizePath(path);
    if (path.getParent() != null) {
      FTPFile[] files = client.listDirectories(normalizePath(path.getParent().toString()));
      return Arrays.stream(files).filter(dir -> filePath.endsWith(dir.getName())).findFirst();
    } else {
      // root directory
      return Optional.ofNullable(createRootFile());
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
   * @return an {@link FTPFile} that represents the root directory of the ftp server
   */
  private FTPFile createRootFile() {
    FTPFile file = new FTPFile();
    file.setName(ROOT);
    file.setType(DIRECTORY_TYPE);
    file.setTimestamp(Calendar.getInstance());
    return file;
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

}
