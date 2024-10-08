/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;

/**
 * Implementation of {@link ConnectionSource} that is based on an instance of {@link FileSystem}, it will always return
 * the same instance when calling {@link ConnectionSource#getConnection()} and it will do nothing when
 * calling {@link ConnectionSource#releaseConnection()}
 */
public class StaticConnectionSource<T extends FileSystem> implements ConnectionSource<T> {

  private T fileSystem;

  public StaticConnectionSource(T fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getConnection() throws ConnectionException {
    return fileSystem;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void releaseConnection() {
    // Nothing is released here, the one who asked for the connection is the one in charge of releasing it.
  }
}
