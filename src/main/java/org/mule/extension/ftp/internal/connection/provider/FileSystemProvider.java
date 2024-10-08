/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection.provider;

import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.RefName;

import java.util.Objects;

/**
 * Base class for a {@link ConnectionProvider} which provides instances of
 * {@link FileSystem}
 *
 * @param <T> The generic type of the file system implementation
 * @since 1.0
 */
public abstract class FileSystemProvider<T extends FileSystem> implements ConnectionProvider<T> {

  @RefName
  private String configName;

  /**
   * @return the name that this config has on the mule registry
   */
  protected String getConfigName() {
    return configName;
  }

  /**
   * The directory to be considered as the root of every relative path used with this connector.
   */
  public abstract String getWorkingDir();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileSystemProvider<?> that = (FileSystemProvider<?>) o;
    return Objects.equals(configName, that.configName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configName);
  }
}
