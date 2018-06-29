/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static java.lang.Thread.sleep;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.file.common.api.exceptions.DeletedFileWhileReadException;
import org.mule.extension.file.common.api.exceptions.FileBeingModifiedException;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.file.common.api.stream.LazyStreamSupplier;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An {@link AbstractFileInputStream} implementation which obtains a {@link FtpFileSystem} through a {@link ConnectionManager} and
 * uses it to obtain the contents of a file on a FTP server.
 * <p>
 * When the stream is closed or fully consumed, the {@link FtpFileSystem} is released back to the {@link ConnectionManager}
 *
 * @since 1.0
 */
public abstract class FtpInputStream extends AbstractFileInputStream {

  protected ConnectionAwareSupplier connectionAwareSupplier;

  protected static ConnectionManager getConnectionManager(FtpConnector config) throws ConnectionException {
    return config.getConnectionManager();
  }

  protected FtpInputStream(ConnectionAwareSupplier connectionAwareSupplier, PathLock lock) throws ConnectionException {
    super(new LazyStreamSupplier(connectionAwareSupplier), lock);
    this.connectionAwareSupplier = connectionAwareSupplier;
  }

  @Override
  protected void doClose() throws IOException {
    try {
      beforeClose();
    } finally {
      try {
        super.doClose();
      } finally {
        connectionAwareSupplier.getConnectionHandler().ifPresent(ConnectionHandler::release);
      }
    }
  }

  /**
   * Template method for performing operations just before the stream is closed. This default implementation is empty.
   *
   * @throws IOException
   */
  protected void beforeClose() throws IOException {}

  /**
   * @return {@link Optional} of the {@link FtpFileSystem} used to obtain the stream
   */
  protected Optional<FtpFileSystem> getFtpFileSystem() {
    return connectionAwareSupplier.getFtpFileSystem();
  }

  protected static final class ConnectionAwareSupplier implements Supplier<InputStream> {

    private static final Logger LOGGER = getLogger(ConnectionAwareSupplier.class);
    private static final String FILE_NO_LONGER_EXISTS_MESSAGE =
        "Error reading file from path %s. It no longer exists at the time of reading.";
    private static final String STARTING_WAIT_MESSAGE = "Starting wait to check if the file size of the file %s is stable.";
    private static final int MAX_SIZE_CHECK_RETRIES = 2;

    private ConnectionHandler<FtpFileSystem> connectionHandler;
    private FtpFileAttributes attributes;
    private ConnectionManager connectionManager;
    private FtpFileSystem ftpFileSystem;
    private Long timeBetweenSizeCheck;
    private FtpConnector config;

    ConnectionAwareSupplier(FtpFileAttributes attributes, ConnectionManager connectionManager,
                            Long timeBetweenSizeCheck, FtpConnector config) {
      this.attributes = attributes;
      this.connectionManager = connectionManager;
      this.timeBetweenSizeCheck = timeBetweenSizeCheck;
      this.config = config;
    }

    @Override
    public InputStream get() {
      try {
        FtpFileAttributes updatedAttributes = getUpdatedAttributes(config, connectionManager, attributes.getPath());
        if (updatedAttributes != null && timeBetweenSizeCheck != null && timeBetweenSizeCheck > 0) {
          updatedAttributes = getUpdatedStableAttributes(config, connectionManager, updatedAttributes);
        }
        if (updatedAttributes == null) {
          throw new DeletedFileWhileReadException(createStaticMessage("File on path " + attributes.getPath()
              + " was read but does not exist anymore."));
        }
        connectionHandler = connectionManager.getConnection(config);
        ftpFileSystem = connectionHandler.getConnection();
        return ftpFileSystem.retrieveFileContent(updatedAttributes);
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    }

    private FtpFileAttributes getUpdatedStableAttributes(FtpConnector config, ConnectionManager connectionManager,
                                                         FtpFileAttributes updatedAttributes)
        throws ConnectionException {
      FtpFileAttributes oldAttributes;
      int retries = 0;
      do {
        oldAttributes = updatedAttributes;
        try {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(STARTING_WAIT_MESSAGE, attributes.getPath()));
          }
          sleep(timeBetweenSizeCheck);
        } catch (InterruptedException e) {
          throw new MuleRuntimeException(createStaticMessage("Execution was interrupted while waiting to recheck file sizes"),
                                         e);
        }
        updatedAttributes = getUpdatedAttributes(config, connectionManager, attributes.getPath());
      } while (updatedAttributes != null && updatedAttributes.getSize() != oldAttributes.getSize()
          && retries++ < MAX_SIZE_CHECK_RETRIES);
      if (retries > MAX_SIZE_CHECK_RETRIES) {
        throw new FileBeingModifiedException(createStaticMessage("File on path " + attributes.getPath()
            + " is still being written."));
      }
      return updatedAttributes;
    }

    private FtpFileAttributes getUpdatedAttributes(FtpConnector config, ConnectionManager connectionManager, String filePath)
        throws ConnectionException {
      ConnectionHandler<FtpFileSystem> connectionHandler = connectionManager.getConnection(config);
      FtpFileSystem ftpFileSystem = connectionHandler.getConnection();
      FtpFileAttributes updatedFtpFileAttributes = ftpFileSystem.getFileAttributes(filePath);
      connectionHandler.release();
      if (updatedFtpFileAttributes == null) {
        LOGGER.error(String.format(FILE_NO_LONGER_EXISTS_MESSAGE, filePath));
      }
      return updatedFtpFileAttributes;
    }

    public Optional<ConnectionHandler> getConnectionHandler() {
      return ofNullable(connectionHandler);
    }

    public Optional<FtpFileSystem> getFtpFileSystem() {
      return ofNullable(ftpFileSystem);
    }

  }

}
