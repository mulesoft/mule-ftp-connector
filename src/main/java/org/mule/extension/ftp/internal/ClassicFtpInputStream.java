/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.lock.PathLock;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;

import java.io.IOException;

/**
 * Implementation of {@link FtpInputStream} for FTP connections
 *
 * @since 1.0
 */
public class ClassicFtpInputStream extends FtpInputStream {

  /**
   * Establishes the underlying connection and returns a new instance of this class.
   * <p>
   * Instances returned by this method <b>MUST</b> be closed or fully consumed.
   *
   * @param config     the {@link FtpConnector} which is configuring the connection
   * @param attributes a {@link FileAttributes} referencing the file which contents are to be fetched
   * @param lock       the {@link PathLock} to be used
   * @return a new {@link FtpInputStream}
   * @throws ConnectionException if a connection could not be established
   */
  public static FtpInputStream newInstance(FtpConnector config, FtpFileAttributes attributes, PathLock lock,
                                           Long timeBetweenSizeCheck)
      throws ConnectionException {
    return new ClassicFtpInputStream(new ConnectionAwareSupplier(attributes, getConnectionManager(config), timeBetweenSizeCheck,
                                                                 config),
                                     lock);
  }

  private ClassicFtpInputStream(ConnectionAwareSupplier connectionAwareSupplier, PathLock lock) throws ConnectionException {
    super(connectionAwareSupplier, lock);
  }

  /**
   * Invokes {@link FtpFileSystem#awaitCommandCompletion()} to make sure that the operation is completed before closing the
   * stream
   */
  @Override
  protected void beforeClose() throws IOException {
    getFtpFileSystem().ifPresent(ftpFileSystem -> ftpFileSystem.awaitCommandCompletion());
  }
}
