/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.ftp;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;

/**
 * Metadata about a file in a FTP server
 *
 * @since 1.0
 */
public class FtpFileAttributes implements Serializable {

  private static final long serialVersionUID = -7637862488391924042L;

  @Parameter
  @Optional
  private LocalDateTime timestamp;

  @Parameter
  private long size;

  @Parameter
  private boolean regularFile;

  @Parameter
  private boolean directory;

  @Parameter
  private boolean symbolicLink;

  @Parameter
  private final String path;

  @Parameter
  private final String fileName;

  /**
   * Creates a new instance of FtpFileAttributes with the specified URI and FTPFile.
   *
   * @param uri     the file's {@link URI}
   * @param ftpFile the {@link FTPFile} which represents the file on the FTP server
   */
  public FtpFileAttributes(URI uri, FTPFile ftpFile) {
    this.path = uri.getPath();
    String name = FilenameUtils.getName(uri.getPath());
    this.fileName = name != null ? name : "";
    timestamp = ftpFile.getTimestamp() != null ? asDateTime(ftpFile.getTimestamp().toInstant()) : null;
    size = ftpFile.getSize();
    regularFile = ftpFile.isFile();
    directory = ftpFile.isDirectory();
    symbolicLink = ftpFile.isSymbolicLink();
  }

  /**
   * Creates a new instance of FtpFileAttributes with default values.
   */
  public FtpFileAttributes() {
    URI uri = createDefaultUri();
    this.path = uri.getPath();
    String name = FilenameUtils.getName(uri.getPath());
    this.fileName = name != null ? name : "";
    timestamp = null;
    size = 0;
    regularFile = false;
    directory = false;
    symbolicLink = false;
  }

  /**
   * Creates a default URI.
   *
   * @return the default {@link URI}
   */
  private static URI createDefaultUri() {
    return URI.create("file:///defaultPath");
  }

  /**
   * Returns the last time the file was modified.
   *
   * @return The last time the file was modified, or {@code null} if such information is not available.
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the last time the file was modified.
   *
   * @param timestamp the timestamp to set
   */
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Returns the size of the file.
   *
   * @return the size of the file
   */
  public long getSize() {
    return size;
  }

  /**
   * Sets the size of the file.
   *
   * @param size the size to set
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * Returns whether the file is a regular file.
   *
   * @return {@code true} if the file is a regular file, {@code false} otherwise
   */
  public boolean isRegularFile() {
    return regularFile;
  }

  /**
   * Returns whether the file is a regular file.
   *
   * @return {@code true} if the file is a regular file, {@code false} otherwise
   */
  public boolean getRegularFile() {
    return regularFile;
  }

  /**
   * Sets whether the file is a regular file.
   *
   * @param regularFile the value to set
   */
  public void setRegularFile(boolean regularFile) {
    this.regularFile = regularFile;
  }

  /**
   * Returns whether the file is a directory.
   *
   * @return {@code true} if the file is a directory, {@code false} otherwise
   */
  public boolean isDirectory() {
    return directory;
  }

  /**
   * Returns whether the file is a directory.
   *
   * @return {@code true} if the file is a directory, {@code false} otherwise
   */
  public boolean getDirectory() {
    return directory;
  }

  /**
   * Sets whether the file is a directory.
   *
   * @param directory the value to set
   */
  public void setDirectory(boolean directory) {
    this.directory = directory;
  }

  /**
   * Returns whether the file is a symbolic link.
   *
   * @return {@code true} if the file is a symbolic link, {@code false} otherwise
   */
  public boolean isSymbolicLink() {
    return symbolicLink;
  }

  /**
   * Returns whether the file is a symbolic link.
   *
   * @return {@code true} if the file is a symbolic link, {@code false} otherwise
   */
  public boolean getSymbolicLink() {
    return symbolicLink;
  }

  /**
   * Sets whether the file is a symbolic link.
   *
   * @param symbolicLink the value to set
   */
  public void setSymbolicLink(boolean symbolicLink) {
    this.symbolicLink = symbolicLink;
  }

  /**
   * Returns the normalized path of the file.
   *
   * @return the normalized path of the file
   */
  public String getPath() {
    return normalizePath(path);
  }

  /**
   * Returns the name of the file.
   *
   * @return the name of the file
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Converts an {@link Instant} to a {@link LocalDateTime}.
   *
   * @param instant the instant to convert
   * @return the corresponding {@link LocalDateTime}
   */
  private LocalDateTime asDateTime(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

}
