/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;

/**
 * Interface used to get and release instances of {@link T}
 *
 * @param <T> the type of fileSystem instances that the implementation will provide
 */
public interface ConnectionSource<T extends FileSystem> {

  /**
   * Gets an instance of {@link T}, multiple calls to this method may return the same instance.
   *
   * @return an instance of {@link T}
   * @throws ConnectionException
   */
  T getConnection() throws ConnectionException;

  /**
   * Release, if needed, the last instance retrieved by calling {@link ConnectionSource#getConnection()}
   */
  void releaseConnection();

}
