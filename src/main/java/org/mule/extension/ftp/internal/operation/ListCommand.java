/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.operation;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.extension.ftp.internal.subset.SubsetList;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.List;
import java.util.function.Predicate;

/**
 * Command design pattern for listing files
 *
 * @since 1.0
 */
public interface ListCommand {

  /**
   * Lists files under the considerations of {@link FileSystem#list(FileConnectorConfig, String, boolean, Predicate)}
   *
   * @param config        the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive     whether to include the contents of sub-directories
   * @param matcher       a {@link Predicate} of {@link FtpFileAttributes} used to filter the output list
   * @return a {@link List} of {@link Result} objects each one containing each file's content in the payload and metadata in the attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  default List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config,
                                                       String directoryPath,
                                                       boolean recursive,
                                                       Predicate<FtpFileAttributes> matcher) {
    return list(config, directoryPath, recursive, matcher);
  }

  /**
   * Lists files under the considerations of {@link FileSystem#list(FileConnectorConfig, String, boolean, Predicate)}
   *
   * @param config        the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive     whether to include the contents of sub-directories
   * @param matcher       a {@link Predicate} of {@link FtpFileAttributes} used to filter the output list
   * @param subsetList    parameter group that lets you obtain a subset of the results
   * @return a {@link List} of {@link Result} objects each one containing each file's content in the payload and metadata in the
   * attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  default List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config,
                                                       String directoryPath,
                                                       boolean recursive,
                                                       Predicate<FtpFileAttributes> matcher,
                                                       SubsetList subsetList) {
    return list(config, directoryPath, recursive, matcher);
  }
}
