/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.operation;

import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.extension.ftp.api.FileWriteMode;

import java.io.InputStream;

/**
 * Command design pattern for writing files
 *
 * @since 1.0
 */
public interface WriteCommand {

  String IS_A_DIRECTORY_MESSAGE = "Is a directory";

  /**
   * Writes a file under the considerations of {@link FileSystem#write(String, InputStream, FileWriteMode, boolean, boolean)}
   *
   * @param filePath the path of the file to be written
   * @param content the content to be written into the file
   * @param mode a {@link FileWriteMode}
   * @param lock whether or not to lock the file
   * @param createParentDirectory whether or not to attempt creating the parent directory if it doesn't exist.
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  default void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectory) {
    write(filePath, content, mode, lock, createParentDirectory);
  }
}
