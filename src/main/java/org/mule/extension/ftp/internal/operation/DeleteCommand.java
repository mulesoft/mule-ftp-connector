/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.operation;

import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.connection.FileSystem;

/**
 * Command design pattern for deleting files
 *
 * @since 1.0
 */
public interface DeleteCommand {

  /**
   * Deletes a file under the considerations of {@link FileSystem#delete(String)}
   *
   * @param filePath the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exist or is locked
   */
  void delete(String filePath);
}
