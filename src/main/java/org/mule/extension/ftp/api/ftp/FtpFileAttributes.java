/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.ftp;

import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import org.mule.extension.file.common.api.AbstractFileAttributes;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Metadata about a file in a FTP server
 *
 * @since 1.0
 */
public class FtpFileAttributes extends AbstractFileAttributes {

  @Parameter
  @Optional
  private LocalDateTime timestamp;

  @Parameter
  // TODO MULE-XXXXX: Remove redundant 'name' attribute in next major version,
  // since it represents the same that 'fileName' from AbstractFileAttributes.
  private String name;

  @Parameter
  private long size;

  @Parameter
  private boolean regularFile;

  @Parameter
  private boolean directory;

  @Parameter
  private boolean symbolicLink;

  /**
   * Creates a new instance
   *
   * @param path the file's {@link Path}
   * @param ftpFile the {@link FTPFile} which represents the file on the FTP server
   */
  public FtpFileAttributes(Path path, FTPFile ftpFile) {
    super(path);
    timestamp = ftpFile.getTimestamp() != null ? asDateTime(ftpFile.getTimestamp().toInstant()) : null;
    // TODO MULE-XXXXX: Remove redundant 'name' attribute in next major version
    name = ftpFile.getName() != null ? ftpFile.getName() : "";
    size = ftpFile.getSize();
    regularFile = ftpFile.isFile();
    directory = ftpFile.isDirectory();
    symbolicLink = ftpFile.isSymbolicLink();
  }

  /**
   * @return The last time the file was modified, or {@code null} if such information is not available.
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getSize() {
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRegularFile() {
    return regularFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDirectory() {
    return directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSymbolicLink() {
    return symbolicLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPath() {
    return normalizePath(super.getPath());
  }
}
