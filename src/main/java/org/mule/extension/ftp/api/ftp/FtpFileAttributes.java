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
  protected final String path;

  @Parameter
  private final String fileName;

  /**
   * Creates a new instance
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

  private static URI createDefaultUri() {
    return URI.create("file:///defaultPath");
  }

  /**
   * @return The last time the file was modified, or {@code null} if such information is not available.
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public boolean isRegularFile() {
    return regularFile;
  }

  public boolean getRegularFile() {
    return regularFile;
  }

  public void setRegularFile(boolean regularFile) {
    this.regularFile = regularFile;
  }

  public boolean isDirectory() {
    return directory;
  }

  public boolean getDirectory() {
    return directory;
  }

  public void setDirectory(boolean directory) {
    this.directory = directory;
  }

  public boolean isSymbolicLink() {
    return symbolicLink;
  }

  public boolean getSymbolicLink() {
    return symbolicLink;
  }

  public void setSymbolicLink(boolean symbolicLink) {
    this.symbolicLink = symbolicLink;
  }

  public String getPath() {
    return normalizePath(path);
  }

  public String getFileName() {
    return fileName;
  }

  protected LocalDateTime asDateTime(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

}
