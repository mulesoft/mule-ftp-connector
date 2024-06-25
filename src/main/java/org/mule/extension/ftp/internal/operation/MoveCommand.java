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
 * Command design pattern for moving files
 *
 * @since 1.0
 */
public interface MoveCommand {

  /**
   * Moves files under the considerations of {@link FileSystem#move(FileConnectorConfig, String, String, boolean, boolean, String)}
   *
   * @param config the config that is parameterizing this operation
   * @param sourcePath the path to the file to be copied
   * @param targetPath the target directory
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param renameTo the new file name, {@code null} if the file doesn't need to be renamed
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories,
            String renameTo);
}
