/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.ftp.DefaultFtpTestHarness;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.DefaultFtpTestHarness.FTP_PASSWORD;
import static org.mule.extension.ftp.DefaultFtpTestHarness.FTP_USER;
import static org.mule.test.extension.file.common.api.FileTestHarness.WORKING_DIR;

import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

public class FtpCommandTestCase {

  private static final String TEMP_DIRECTORY = "files";

  @ClassRule
  public static DefaultFtpTestHarness testHarness = new DefaultFtpTestHarness();

  private FtpWriteCommand ftpWriteCommand;
  private FtpListCommand ftpListCommand;
  private FtpReadCommand ftpReadCommand;
  private FTPClient client;

  private static final String fileName = "NewFile.txt";
  private static final String filePath = "/" + TEMP_DIRECTORY + "/" + fileName;
  private static final String fullPath = "/" + WORKING_DIR + filePath;
  private static final String fileContent = "File Content.";

  @Before
  public void setUp() throws Exception {
    testHarness.makeDir(TEMP_DIRECTORY);
    testHarness.write(fullPath, fileContent);

    client = spy(FTPClient.class);
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);
  }

  @Test
  public void listRecentlyCreatedDirectory() throws Exception {
    ftpWriteCommand = new FtpWriteCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    assertThat(ftpWriteCommand.getFile(TEMP_DIRECTORY), is(notNullValue()));
  }

  @Test
  public void getFileAttributesFromServerThatDoesNotSupportMLSTCommand()
      throws Exception {
    when(client.features()).thenReturn(false); // To disable MLST support.

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(0)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(any());
  }

  @Test
  public void getFileAttributesFromServerThatIncorrectlySupportsMLSTCommand() throws Exception {
    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(any());
  }

  @Test
  public void getFileAttributesFromServerThatSupportsMLSTCommand() throws Exception {
    /* We need to mock the call to the 'mlistFile' method because the server we use has a bug and replies with a
    non-compliant response. See https://issues.apache.org/jira/browse/FTPSERVER-480. */
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName("NewFile.txt");
    doReturn(ftpFile).when(client).mlistFile(any());

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(0)).initiateListParsing(any());
  }

  @Test
  public void listDirectoryFromServerThatDoesNotSupportMLSDCommand() throws Exception {
    // By default the test server does not support MLSD.

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, times(0)).mlistDir(any());
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void listDirectoryFromServerThatIncorrectlySupportsMLSDCommand() throws Exception {
    // Mocking incorrect support:
    when(client.getReplyStrings()).thenReturn(new String[] {"MLSD"}).thenReturn(new String[] {"MLSD"}).thenCallRealMethod();
    when(client.mlistDir()).thenReturn(null);
    when(client.getReplyCode()).thenReturn(522).thenCallRealMethod();

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, times(2)).mlistDir(); // Mockito adds an extra call.
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void listDirectoryFromServerThatSupportsMLSDCommand() throws Exception {
    // Mock MLSD support:
    when(client.getReplyStrings()).thenReturn(new String[] {"MLSD"}).thenReturn(new String[] {"MLSD"}).thenCallRealMethod();
    FTPFile file = new FTPFile();
    file.setName(TEMP_DIRECTORY);
    file.setType(FTPFile.DIRECTORY_TYPE);
    when(client.mlistDir()).thenReturn(new FTPFile[] {file});

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, times(2)).mlistDir(); // Mockito adds an extra call.
    verify(client, times(0)).initiateListParsing();
  }

}
