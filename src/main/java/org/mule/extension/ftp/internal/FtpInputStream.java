/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.ConnectionSource;
import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.extension.ftp.internal.connection.ManagerBasedConnectionSource;
import org.mule.extension.ftp.internal.connection.StaticConnectionSource;
import org.mule.extension.ftp.api.DeletedFileWhileReadException;
import org.mule.extension.ftp.api.FileBeingModifiedException;
import org.mule.extension.ftp.internal.lock.UriLock;
import org.mule.extension.ftp.internal.stream.AbstractNonFinalizableFileInputStream;
import org.mule.extension.ftp.internal.stream.ExceptionInputStream;
import org.mule.extension.ftp.internal.stream.LazyStreamSupplier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * An {@link AbstractNonFinalizableFileInputStream} implementation which obtains a {@link FtpFileSystem} through a
 * {@link ConnectionManager} and uses it to obtain the contents of a file on a FTP server.
 * <p>
 * When the stream is closed or fully consumed, the {@link FtpFileSystem} is released back to the {@link ConnectionManager}
 *
 * @since 1.0
 */
public abstract class FtpInputStream extends AbstractNonFinalizableFileInputStream {

  protected FtpFileInputStreamSupplier ftpFileInputStreamSupplier;

  protected static ConnectionManager getConnectionManager(FtpConnector config) throws ConnectionException {
    return config.getConnectionManager();
  }

  protected FtpInputStream(FtpFileInputStreamSupplier ftpFileInputStreamSupplier, UriLock lock) throws ConnectionException {
    super(new LazyStreamSupplier(ftpFileInputStreamSupplier), lock);
    this.ftpFileInputStreamSupplier = ftpFileInputStreamSupplier;
  }

  @Override
  protected void doClose() throws IOException {
    try {
      super.doClose();
    } finally {
      try {
        beforeConnectionRelease();
      } finally {
        ftpFileInputStreamSupplier.releaseConnectionUsedForContentInputStream();
      }
    }
  }

  /**
   * Template method for performing operations just after the stream is closed but before the connection is released. This default
   * implementation is empty.
   *
   * @throws IOException
   */
  protected void beforeConnectionRelease() throws IOException {}

  /**
   * @return {@link Optional} of the {@link FtpFileSystem} used to obtain the stream
   */
  protected Optional<FtpFileSystem> getFtpFileSystem() {
    try {
      return ftpFileInputStreamSupplier.getConnectionUsedForContentInputStream();
    } catch (ConnectionException e) {
      return empty();
    }
  }

  /**
   * This is a concrete class that implements {@link Supplier<InputStream>} adding the logic to check that the file size is stable.
   * Its contain implementations of {@link FtpFileInputStreamSupplier#getUpdatedAttributes()}, to be able to get
   * the size of the file, and {@link FtpFileInputStreamSupplier#getContentInputStream()}, to get the InputStream that is
   * supplied.
   *
   * @since 1.2
   */
  protected static class FtpFileInputStreamSupplier implements Supplier<InputStream> {

    private static final Logger LOGGER = getLogger(FtpFileInputStreamSupplier.class);
    private static final String STARTING_WAIT_MESSAGE = "Starting wait to check if the file size of the file %s is stable.";
    private static final String FILE_NO_LONGER_EXISTS_MESSAGE =
        "Error reading file from path %s. It no longer exists at the time of reading.";
    private static final int MAX_SIZE_CHECK_RETRIES = 2;
    private static final AtomicBoolean alreadyLoggedWarning = new AtomicBoolean();
    private static final String WAIT_WARNING_MESSAGE =
        "With the purpouse of performing a size check on the file %s, this thread will sleep. The connector has no control of" +
            " which type of thread the sleep will take place on, this can lead to running out of thread if the time for " +
            "'timeBetweenSizeCheck' is big or a lot of files are being read concurrently. This warning will only be shown once.";
    private ConnectionSource<FtpFileSystem> connectionSource;
    private boolean contentProvided = false;
    private boolean contentConnectionReleased = false;

    protected FtpFileAttributes attributes;
    private Long timeBetweenSizeCheck;

    FtpFileInputStreamSupplier(FtpFileAttributes attributes, ConnectionManager connectionManager,
                               Long timeBetweenSizeCheck, FtpConnector config) {
      this.attributes = attributes;
      this.timeBetweenSizeCheck = timeBetweenSizeCheck;
      this.connectionSource = new ManagerBasedConnectionSource<>(config, connectionManager);
    }

    FtpFileInputStreamSupplier(FtpFileAttributes attributes, Long timeBetweenSizeCheck, FtpFileSystem fileSystem) {
      this.attributes = attributes;
      this.timeBetweenSizeCheck = timeBetweenSizeCheck;
      this.connectionSource = new StaticConnectionSource<>(fileSystem);
    }

    /**
     * Gets the updated attributes of the file.
     *
     * @param fileSystem the {@link FileSystem} to be used to gather the updated attributes
     * @return the updated attributes according to the path of the variable attributes passed in the constructor
     */
    private FtpFileAttributes getUpdatedAttributes(FtpFileSystem fileSystem) {
      return fileSystem.getFileAttributes(attributes.getPath());
    }

    /**
     * Gets the {@link InputStream} of the file described by the attributes passed to the constructor
     *
     * @param fileSystem the {@link FileSystem} to be used to get the content of the file
     * @return the {@link InputStream} of the file
     */
    private InputStream getContentInputStream(FtpFileSystem fileSystem) {
      return fileSystem.retrieveFileContent(attributes);
    }

    /**
     * This method will be called when a {@link MuleRuntimeException} is thrown while retrieving the content of the file
     * and its implementation will return whether the file was deleted or not based on the exception thrown.
     *
     * @param e the thrown {@link MuleRuntimeException}
     * @return whether the exception implies that the file to be read was deleted
     */
    private boolean fileWasDeleted(MuleRuntimeException e) {
      return e.getCause() instanceof FileNotFoundException;
    }

    /**
     * Gets the updated attributes of the file.
     *
     * @return the updated attributes accourding to the path of the variable attributes passed in the constructor
     */
    private FtpFileAttributes getUpdatedAttributes() {
      try {
        FtpFileSystem fileSystem = connectionSource.getConnection();
        FtpFileAttributes updatedFileAttributes = getUpdatedAttributes(fileSystem);
        releaseConnection();
        if (updatedFileAttributes == null) {
          LOGGER.error(String.format(FILE_NO_LONGER_EXISTS_MESSAGE, attributes.getPath()));
        }
        return updatedFileAttributes;
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    }

    /**
     * Gets the {@link InputStream} of the file described by the attributes passed to the constructor
     *
     * @return the {@link InputStream} of the file
     */
    private InputStream getContentInputStream() {
      try {
        InputStream content = getContentInputStream(connectionSource.getConnection());
        contentProvided = true;
        return content;
      } catch (MuleRuntimeException e) {
        if (fileWasDeleted(e)) {
          onFileDeleted(e);
        }
        throw e;
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                       e);
      }
    }

    /**
     * If the content of the file was retrieved, this method will release the connection used to get that content.
     */
    public void releaseConnectionUsedForContentInputStream() {
      if (contentProvided && !contentConnectionReleased) {
        releaseConnection();
        contentConnectionReleased = true;
      }
    }

    private void releaseConnection() {
      connectionSource.releaseConnection();
    }

    /**
     * If the content of the file was retrieved, this method will return the {@link FileSystem} used to retrieve that
     * content.
     *
     * @return an {@link Optional} that contains the {@link FileSystem} or {@link Optional#empty()}
     * @throws ConnectionException
     */
    public Optional<FtpFileSystem> getConnectionUsedForContentInputStream() throws ConnectionException {
      return contentProvided && !contentConnectionReleased ? of(connectionSource.getConnection()) : empty();
    }

    @Override
    public InputStream get() {
      FtpFileAttributes updatedAttributes = null;
      if (timeBetweenSizeCheck != null && timeBetweenSizeCheck > 0) {
        updatedAttributes = getUpdatedStableAttributes();
        if (updatedAttributes == null) {
          onFileDeleted();
        }
      }
      try {
        return getContentInputStream();
      } catch (RuntimeException e) {
        return new ExceptionInputStream(e);
      }
    }

    private FtpFileAttributes getUpdatedStableAttributes() {
      FtpFileAttributes oldAttributes;
      FtpFileAttributes updatedAttributes = attributes;
      int retries = 0;
      do {
        oldAttributes = updatedAttributes;
        try {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format(STARTING_WAIT_MESSAGE, attributes.getPath()));
          }
          if (alreadyLoggedWarning.compareAndSet(false, true)) {
            LOGGER.warn(format(WAIT_WARNING_MESSAGE, attributes.getPath()));
          }
          sleep(timeBetweenSizeCheck);
        } catch (InterruptedException e) {
          throw new MuleRuntimeException(createStaticMessage("Execution was interrupted while waiting to recheck file sizes"),
                                         e);
        }
        updatedAttributes = getUpdatedAttributes();
      } while (updatedAttributes != null && updatedAttributes.getSize() != oldAttributes.getSize()
          && retries++ <= MAX_SIZE_CHECK_RETRIES);
      if (retries > MAX_SIZE_CHECK_RETRIES) {
        throw new FileBeingModifiedException(createStaticMessage("File on path " + attributes.getPath()
            + " is still being written."));
      }
      return updatedAttributes;
    }

    private void onFileDeleted() {
      throw new DeletedFileWhileReadException(createStaticMessage("File on path " + attributes.getPath()
          + " was read but does not exist anymore."));
    }

    private void onFileDeleted(Exception e) {
      throw new DeletedFileWhileReadException(createStaticMessage("File on path " + attributes.getPath()
          + " was read but does not exist anymore."), e);
    }
  }
}
