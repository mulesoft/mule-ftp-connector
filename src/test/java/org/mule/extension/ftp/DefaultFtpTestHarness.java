/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.ftp.api.UriUtils.createUri;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;

import org.mule.extension.AbstractFtpTestHarness;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.extension.ftp.api.FileTestHarness;
import org.mule.test.infrastructure.client.ftp.FTPTestClient;
import org.mule.test.infrastructure.process.rules.FtpServer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.rules.TestRule;

/**
 * Implementation of {@link FileTestHarness} for classic FTP connections
 *
 * @since 1.0
 */
public class DefaultFtpTestHarness extends AbstractFtpTestHarness {

  public static final String FTP_USER = "anonymous";
  public static final String FTP_PASSWORD = "password";

  private final File serverBaseDir = new File(FTP_SERVER_BASE_DIR, WORKING_DIR);
  private FtpServer ftpServer = new FtpServer("ftpPort", serverBaseDir);
  private SystemProperty workingDirSystemProperty = new SystemProperty(WORKING_DIR_SYSTEM_PROPERTY, WORKING_DIR);
  private FTPTestClient ftpClient;


  /**
   * Creates a new instance activating the {@code ftp} spring profile
   */
  public DefaultFtpTestHarness() {
    super("ftp");
  }

  /**
   * Starts a FTP server and connects a client to it
   */
  @Override
  protected void doBefore() throws Exception {
    ftpServer.start();
    ftpClient = new FTPTestClient(DEFAULT_FTP_HOST, ftpServer.getPort(), FTP_USER, FTP_PASSWORD);

    if (!ftpClient.testConnection()) {
      throw new IOException("could not connect to ftp server");
    }
    ftpClient.changeWorkingDirectory(WORKING_DIR);
  }

  /**
   * Disconnects the client and shuts the server down
   */
  @Override
  protected void doAfter() throws Exception {
    try {
      if (ftpClient.isConnected()) {
        ftpClient.disconnect();
      }
    } finally {
      ftpServer.stop();
      FileUtils.deleteDirectory(serverBaseDir);
    }
  }

  /**
   * @return {@link #workingDirSystemProperty , and {@link #ftpServer}}
   */
  @Override
  protected TestRule[] getChildRules() {
    return new TestRule[] {workingDirSystemProperty, ftpServer};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createHelloWorldFile() throws Exception {
    ftpClient.makeDir("files");
    ftpClient.putFile("files/" + HELLO_FILE_NAME, HELLO_WORLD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createBinaryFile() throws Exception {
    ftpClient.putFile(BINARY_FILE_NAME, HELLO_WORLD.getBytes());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void makeDir(String directoryPath) throws Exception {
    ftpClient.makeDir(directoryPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWorkingDirectory() throws Exception {
    return normalizePath(ftpClient.getWorkingDirectory());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String path, String content) throws Exception {
    ftpClient.putFile(path, content);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean dirExists(String path) throws Exception {
    return ftpClient.dirExists(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean fileExists(String path) throws Exception {
    return ftpClient.fileExists(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean changeWorkingDirectory(String path) throws Exception {
    return ftpClient.changeWorkingDirectory(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getFileList(String path) throws Exception {
    return ftpClient.getFileList(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getServerPort() throws Exception {
    return ftpServer.getPort();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertAttributes(String path, Object attributes) throws Exception {
    FtpFileAttributes ftpFileAttributes = (FtpFileAttributes) attributes;
    FTPFile file = ftpClient.get(path);

    assertThat(ftpFileAttributes.getName(), equalTo(file.getName()));
    assertThat(createUri(ftpFileAttributes.getPath()).getPath(),
               equalTo(createUri(String.format("/%s/%s", WORKING_DIR, HELLO_PATH)).getPath()));
    assertThat(ftpFileAttributes.getSize(), is(file.getSize()));
    assertTime(ftpFileAttributes.getTimestamp(), file.getTimestamp(), 1);
    assertThat(ftpFileAttributes.isDirectory(), is(false));
    assertThat(ftpFileAttributes.isSymbolicLink(), is(false));
    assertThat(ftpFileAttributes.isRegularFile(), is(true));
  }

  private void assertTime(LocalDateTime dateTime, Calendar calendar, long toleranceMicroseconds) {
    Instant expectedInstant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
    Instant actualInstant = calendar.toInstant();
    long differenceMicroseconds = Duration.between(expectedInstant, actualInstant).toNanos() / 1000;

    // The FTP server MLST is less precise than the Java Calendar, so we need to allow for some tolerance
    assertThat(differenceMicroseconds, is(lessThanOrEqualTo(toleranceMicroseconds)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assertDeleted(String path) throws Exception {
    assertThat(fileExists(path), is(false));
  }

  public void setTimestamp(String path, LocalDateTime time) throws Exception {
    ftpClient.setTimestamp(path, time);
  }

  protected void writeByteByByteAsync(String path, String content, long delayBetweenCharacters) throws Exception {

    new Thread(() -> {
      try {
        for (int i = 1; i <= content.length(); i++) {
          ftpClient.putFile(path, content.substring(0, i));
          sleep(delayBetweenCharacters);
        }
      } catch (Exception e) {
        fail("Error trying to write in file");
      }
    }).start();

  }

}
