/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.extension.ftp.api.FileError.DISCONNECTED;
import static org.mule.extension.ftp.api.UriUtils.createUri;
import static org.mule.extension.ftp.internal.FtpUtils.createUrl;
import static org.mule.extension.ftp.internal.FtpUtils.getReplyCodeErrorMessage;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.ftp.api.FileWriteMode;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.exception.FileLockedException;
import org.mule.extension.ftp.internal.operation.CopyCommand;
import org.mule.extension.ftp.internal.operation.CreateDirectoryCommand;
import org.mule.extension.ftp.internal.operation.DeleteCommand;
import org.mule.extension.ftp.internal.operation.ListCommand;
import org.mule.extension.ftp.internal.operation.MoveCommand;
import org.mule.extension.ftp.internal.operation.ReadCommand;
import org.mule.extension.ftp.internal.operation.RenameCommand;
import org.mule.extension.ftp.internal.operation.WriteCommand;
import org.mule.extension.ftp.internal.lock.URLPathLock;
import org.mule.extension.ftp.internal.lock.UriLock;
import org.mule.extension.ftp.internal.subset.SubsetList;
import org.mule.extension.ftp.api.UriUtils;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.api.ftp.FtpTransferMode;
import org.mule.extension.ftp.internal.command.FtpCopyCommand;
import org.mule.extension.ftp.internal.command.FtpCreateDirectoryCommand;
import org.mule.extension.ftp.internal.command.FtpDeleteCommand;
import org.mule.extension.ftp.internal.command.FtpListCommand;
import org.mule.extension.ftp.internal.command.FtpMoveCommand;
import org.mule.extension.ftp.internal.command.FtpReadCommand;
import org.mule.extension.ftp.internal.command.FtpRenameCommand;
import org.mule.extension.ftp.internal.command.FtpWriteCommand;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.slf4j.Logger;

/**
 * Implementation of {@link FileSystem} for files residing on a FTP server
 *
 * @since 1.0
 */
public class FtpFileSystem implements FileSystem {

  private static final Logger LOGGER = getLogger(FtpFileSystem.class);
  private SingleFileListingMode singleFileListingMode = SingleFileListingMode.UNSET;
  private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

  private static String resolveBasePath(String basePath, FTPClient client) {
    if (isBlank(basePath)) {
      try {
        return client.printWorkingDirectory();
      } catch (Exception e) {
        LOGGER.error("FTP working dir was not specified and failed to resolve a default one", e);
        throw new MuleRuntimeException(createStaticMessage("FTP working dir was not specified and failed to resolve a default one"),
                                       e);
      }
    }
    return UriUtils.createUri("", basePath).getPath();
  }

  private final FTPClient client;
  private final CopyCommand copyCommand;
  private final CreateDirectoryCommand createDirectoryCommand;
  private final DeleteCommand deleteCommand;
  private final ListCommand listCommand;
  private final MoveCommand moveCommand;
  private final ReadCommand readCommand;
  private final RenameCommand renameCommand;
  private final WriteCommand writeCommand;
  @Inject
  private final LockFactory lockFactory;
  private final String basePath;

  /**
   * Creates a new instance
   *
   * @param client a ready to use {@link FTPClient}
   */
  public FtpFileSystem(FTPClient client, String basePath, LockFactory lockFactory, SingleFileListingMode singleFileListingMode) {
    this.basePath = resolveBasePath(basePath, client);
    this.client = client;
    this.lockFactory = lockFactory;
    this.singleFileListingMode = singleFileListingMode;

    copyCommand = new FtpCopyCommand(this, client);
    createDirectoryCommand = new FtpCreateDirectoryCommand(this, client);
    deleteCommand = new FtpDeleteCommand(this, client);
    moveCommand = new FtpMoveCommand(this, client);
    readCommand = new FtpReadCommand(this, client);
    listCommand = new FtpListCommand(this, client, (FtpReadCommand) readCommand);
    renameCommand = new FtpRenameCommand(this, client);
    writeCommand = new FtpWriteCommand(this, client);
  }

  /**
   * Severs the connection by invoking {@link FTPClient#logout()} and {@link FTPClient#disconnect()} on the provided
   * {@link #client}.
   * <p>
   * Notice that {@link FTPClient#disconnect()} will be invoked even if {@link FTPClient#logout()} fails. This method will never
   * throw exception. Any errors will be logged.
   */
  public void disconnect() {
    try {
      client.logout();
    } catch (FTPConnectionClosedException e) {
      // this is valid and expected if the server closes the connection prematurely as a result of the logout... ignore
    } catch (Exception e) {
      LOGGER.error("Exception found trying to logout from ftp at {} ", toURL(createUri("")), e);
      LOGGER.debug(e.getMessage(), e);
    } finally {
      try {
        client.disconnect();
      } catch (Exception e) {
        LOGGER.error("Exception found trying to disconnect from ftp at {} ", toURL(createUri("")), e);
        LOGGER.debug(e.getMessage(), e);
      }
    }
  }

  public boolean isFeatureSupported(String command) {
    try {
      return client.hasFeature(command);
    } catch (IOException exception) {
      LOGGER.error(format("Unable to resolve if feature {} is supported.", command), exception);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  private boolean isConnected() {
    return client.isConnected();
  }

  /**
   * Validates the underlying connection to the remote server
   *
   * @return a {@link ConnectionValidationResult}
   */
  public ConnectionValidationResult validateConnection() {
    if (!isConnected()) {
      LOGGER.trace("Connection validation failed.");

      return failure("Connection is stale", new FTPConnectionException("Connection is stale", DISCONNECTED));
    }

    try {
      changeToBaseDir();
    } catch (Exception e) {
      LOGGER.error("Error occurred while changing to base directory {}", getBasePath(), e);
      return failure("Configured workingDir is unavailable", e);
    }
    return success();
  }

  /**
   * Sets the transfer mode on the {@link #client}
   *
   * @param mode a {@link FtpTransferMode}
   */
  public void setTransferMode(FtpTransferMode mode) {
    try {
      if (!client.setFileType(mode.getCode())) {
        throw new IOException(format("Failed to set %s transfer type. %s", mode.getDescription(),
                                     getReplyCodeErrorMessage(client.getReplyCode())));
      }
    } catch (Exception e) {
      LOGGER.error(format("Found exception trying to change transfer mode to %s. %s",
                          mode.getClass(),
                          getReplyCodeErrorMessage(client.getReplyCode())),
                   e);
      throw new MuleRuntimeException(createStaticMessage(format("Found exception trying to change transfer mode to %s. %s",
                                                                mode.getClass(),
                                                                getReplyCodeErrorMessage(client.getReplyCode()))),
                                     e);
    }
  }

  /**
   * Sets the data timeout property on the underlying {@link #client}
   *
   * @param timeout a timeout scalar
   * @param timeUnit a {@link TimeUnit} which qualifies the {@code timeout}
   */
  public void setResponseTimeout(Integer timeout, TimeUnit timeUnit) {
    client.setDataTimeout(new Long(timeUnit.toMillis(timeout)).intValue());
  }

  /**
   * If {@code passive} is {@code true} then the {@link #client} is set on passive mode. Otherwise is set on active mode.
   *
   * @param passive whether to go passive mode or not
   */
  public void setPassiveMode(boolean passive) {
    if (passive) {
      LOGGER.debug("Entering FTP passive mode");
      client.enterLocalPassiveMode();
    } else {
      LOGGER.debug("Entering FTP active mode");
      client.enterLocalActiveMode();
    }
  }

  /**
   * Returns an InputStream which obtains the content for the file of the given {@code filePayload}.
   * <p>
   * The invoked <b>MUST</b> make sure that the returned stream is closed in order for the underlying connection to be closed.
   *
   * @param filePayload a {@link FtpFileAttributes} referencing to a FTP file
   * @return an {@link InputStream}
   */
  public InputStream retrieveFileContent(FtpFileAttributes filePayload) {
    try {
      InputStream inputStream = client.retrieveFileStream(normalizePath(filePayload.getPath()));
      if (inputStream == null) {
        throw new FileNotFoundException(format("Could not retrieve content of file '%s' because it doesn't exist",
                                               filePayload.getPath()));
      }

      return inputStream;
    } catch (Exception e) {
      LOGGER.error(format("Exception was found trying to retrieve the contents of file '%s'. %s",
                          filePayload.getPath(),
                          getReplyCodeErrorMessage(client.getReplyCode())),
                   e);
      throw new MuleRuntimeException(createStaticMessage(format("Exception was found trying to retrieve the contents of file '%s'. %s",
                                                                filePayload.getPath(),
                                                                getReplyCodeErrorMessage(client.getReplyCode()))),
                                     e);
    }
  }

  /**
   * Awaits for the underlying {@link #client} to complete any pending commands. This is necessary for certain operations such as
   * write. Using the {@link #client} before that can result in unexpected behavior
   */
  public void awaitCommandCompletion() {
    try {
      if (!client.completePendingCommand()) {
        throw new IllegalStateException("Pending command did not complete");
      }
    } catch (IllegalStateException | IOException e) {
      LOGGER.error(format("Failed to complete pending command. %s",
                          getReplyCodeErrorMessage(client.getReplyCode())),
                   e);
      throw new MuleRuntimeException(createStaticMessage(format("Failed to complete pending command. %s",
                                                                getReplyCodeErrorMessage(client.getReplyCode()))),
                                     e);
    }
  }

  private UriLock createLock(URI uri) {
    return new URLPathLock(toURL(uri), lockFactory);
  }

  private URL toURL(URI uri) {
    try {
      return createUrl(client, uri);
    } catch (MalformedURLException e) {
      LOGGER.error(format("Could not get URL for FTP server %s", uri.getHost()), e);
      throw new MuleRuntimeException(createStaticMessage("Could not get URL for FTP server"), e);
    }
  }

  @Override
  public List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config, String directoryPath,
                                                      boolean recursive, Predicate<FtpFileAttributes> matcher) {
    return getListCommand().list(config, directoryPath, recursive, matcher);
  }

  @Override
  public List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config, String directoryPath,
                                                      boolean recursive, Predicate<FtpFileAttributes> matcher,
                                                      SubsetList subsetList) {
    return getListCommand().list(config, directoryPath, recursive, matcher);
  }

  @Override
  public Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, String filePath,
                                                     boolean lock, Long timeBetweenSizeCheck) {
    return getReadCommand().read(config, filePath, lock, timeBetweenSizeCheck);
  }

  @Override
  public void write(String filePath, InputStream content, FileWriteMode mode,
                    boolean lock, boolean createParentDirectories) {
    getWriteCommand().write(filePath, content, mode, lock, createParentDirectories);
  }

  @Override
  public void copy(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                   boolean createParentDirectories, String renameTo) {
    getCopyCommand().copy(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo);
  }

  @Override
  public void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                   boolean createParentDirectories, String renameTo) {
    getMoveCommand().move(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo);
  }

  @Override
  public void delete(String filePath) {
    getDeleteCommand().delete(filePath);
  }

  @Override
  public void rename(String filePath, String newName, boolean overwrite) {
    getRenameCommand().rename(filePath, newName, overwrite);
  }

  @Override
  public void createDirectory(String directoryPath) {
    getCreateDirectoryCommand().createDirectory(directoryPath);
  }

  @Override
  public Lock createMuleLock(String id) {
    return lockFactory.createLock(id);
  }

  @Override
  public MediaType getFileMessageMediaType(FtpFileAttributes attributes) {
    return MediaType.parse(mimetypesFileTypeMap.getContentType(attributes.getPath()));
  }

  /**
   * Changes the {@link #client}'s current working directory to the base path
   */
  @Override
  public void changeToBaseDir() {
    String basePath = getBasePath();
    if (basePath != null) {
      try {
        client.changeWorkingDirectory(normalizePath(createUri("/", getBasePath()).getPath()));
      } catch (IOException e) {
        LOGGER.error(format("Failed to perform CWD to the base directory '%s'", basePath), e);
        ConnectionException ce = new ConnectionException(e, client);
        throw new MuleRuntimeException(createStaticMessage(format("Failed to perform CWD to the base directory '%s'",
                                                                  basePath)),
                                       ce);
      }
    }
  }

  @Override
  public String getBasePath() {
    return basePath;
  }

  public FTPClient getClient() {
    return client;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ReadCommand getReadCommand() {
    return readCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ListCommand getListCommand() {
    return listCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteCommand getWriteCommand() {
    return writeCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CopyCommand getCopyCommand() {
    return copyCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MoveCommand getMoveCommand() {
    return moveCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteCommand getDeleteCommand() {
    return deleteCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RenameCommand getRenameCommand() {
    return renameCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CreateDirectoryCommand getCreateDirectoryCommand() {
    return createDirectoryCommand;
  }

  /**
   * Obtains a {@link FtpFileAttributes} for the given {@code filePath}
   *
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes} or {@code null} if it doesn't exist
   */
  public FtpFileAttributes getFileAttributes(String filePath) {
    return ((FtpReadCommand) readCommand).getFile(filePath);
  }

  public void setSingleFileListingMode(SingleFileListingMode singleFileListingMode) {
    LOGGER.debug("Setting singleFileListingMode = {}", singleFileListingMode);
    this.singleFileListingMode = singleFileListingMode;
  }

  public SingleFileListingMode getSingleFileListingMode() {
    LOGGER.debug("Current singleFileListingMode = {}", singleFileListingMode);
    return this.singleFileListingMode;
  }

  /**
   * Acquires and returns lock over the given {@code uri}.
   * <p>
   * Depending on the underlying filesystem, the extent of the lock will depend on the implementation. If a lock can not be
   * acquired, then an {@link IllegalStateException} is thrown.
   * <p>
   * Whoever request the lock <b>MUST</b> release it as soon as possible.
   *
   * @param uri   the uri to the file you want to lock
   * @return an acquired {@link UriLock}
   * @throws IllegalArgumentException if a lock could not be acquired
   */
  public final synchronized UriLock lock(URI uri) {
    UriLock lock = createLock(uri);
    acquireLock(lock);

    return lock;
  }

  /**
   * Attempts to lock the given {@code lock} and throws {@link FileLockedException} if already locked
   *
   * @param lock the {@link UriLock} to be acquired
   * @throws FileLockedException if the {@code lock} is already acquired
   */
  private void acquireLock(UriLock lock) {
    if (!lock.tryLock()) {
      throw new FileLockedException(
                                    format("Could not lock file '%s' because it's already owned by another process",
                                           lock.getUri().getPath()));
    }
  }

  /**
   * Verify that the given {@code uri} is not locked
   *
   * @param uri the uri to test
   * @throws IllegalStateException if the {@code uri} is indeed locked
   */
  public void verifyNotLocked(URI uri) {
    if (isLocked(uri)) {
      throw new FileLockedException(format("File '%s' is locked by another process", uri));
    }
  }

  /**
   * Try to acquire a lock on a file and release it immediately. Usually used as a quick check to see if another process is still
   * holding onto the file, e.g. a large file (more than 100MB) is still being written to.
   */
  private boolean isLocked(URI uri) {
    UriLock lock = createLock(uri);
    try {
      return !lock.tryLock();
    } finally {
      lock.release();
    }
  }
}
