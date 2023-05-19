/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import org.apache.commons.net.MalformedServerReplyException;
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

import static org.apache.commons.net.ftp.FTPCmd.MLST;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
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
  public void getFileAttributesFromServerThatDoesNotSupportMLSTCommandWithMalformedServerReplyExceptionResponse()
      throws Exception {
    doThrow(new MalformedServerReplyException()).when(client).mlistFile(any());

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void getFileAttributesFromServerThatDoesNotSupportMLSTCommandWithNullResponse()
      throws Exception {
    doReturn(null).when(client).mlistFile(any());

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    FtpFileAttributes file = ftpReadCommand.getFile(fullPath);

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void getFileAttributesFromServerThatSupportsMLSTCommand()
      throws Exception {
    // Although the server we use supports MLST by default, it has a bug and returns a non-complaint response to the
    // command. See https://issues.apache.org/jira/browse/FTPSERVER-480. Therefore we must mock its behaviour.
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName(fileName);
    doReturn(ftpFile).when(client).mlistFile(any());

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    FtpFileAttributes file = ftpReadCommand.getFile(fullPath);

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(0)).initiateListParsing();
  }

  @Test
  public void listDirectoryFromServerThatDoesNotSupportMLSDCommandWithNegativeCompletion() throws Exception {
    doReturn(new FTPFile[0]).when(client).mlistDir();
    doReturn(522).doCallRealMethod().when(client).getReplyCode();

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, times(1)).mlistDir();
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void listDirectoryFromServerThatDoesNotSupportMLSDCommandWithMalformedServerReplyException() throws Exception {
    doThrow(new MalformedServerReplyException()).when(client).mlistDir();

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, atLeastOnce()).hasFeature(MLST.getCommand());
    verify(client, times(1)).mlistDir();
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void listDirectoryFromServerThatDoesNotListMLSDasFeatureFallsBackToListCommand() throws Exception {
    when(client.hasFeature(MLST.getCommand())).thenReturn(false);

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, atLeastOnce()).hasFeature(MLST.getCommand());
    verify(client, times(0)).mlistDir();
    verify(client, times(1)).initiateListParsing();
  }

  @Test
  public void listDirectoryFromServerThatSupportsMLSDCommand() throws Exception {
    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, atLeastOnce()).hasFeature(MLST.getCommand());
    verify(client, times(1)).mlistDir();
    verify(client, times(0)).initiateListParsing();
  }

  @Test
  public void testThatGetFileFromParentDirectoryReturnsFileEvenIfThereIsNotMLSTCommand() throws Exception {
    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR + "/files", mock(LockFactory.class)), client, ftpReadCommand);

    for (int i = 0; i < 9; i++) {
      String newPath = fullPath.substring(0, fullPath.length() - 4) + i;
      newPath = newPath.concat(".txt");
      testHarness.write(newPath, fileContent);
    }

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");
    String[] files = testHarness.getFileList(TEMP_DIRECTORY);
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    assertThat(files.length, is(10));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(anyString());
  }

  @Test
  public void testThatGetInvalidFileFromParentDirectoryReturnsNullEvenIfThereIsNotMLSTCommand() throws Exception {
    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR + "/files", mock(LockFactory.class)), client, ftpReadCommand);

    for (int i = 0; i < 9; i++) {
      String newPath = fullPath.substring(0, fullPath.length() - 4) + i;
      newPath = newPath.concat(".txt");
      testHarness.write(newPath, fileContent);
    }

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/invalidFile.txt");
    String[] files = testHarness.getFileList(TEMP_DIRECTORY);
    assertThat(file, is(nullValue()));
    assertThat(files.length, is(10));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(anyString());
  }

  @Test
  public void testThatGetInvalidFileFromParentDirectoryReturnsFileEvenIfThereIsNotMLSTCommand() throws Exception {
    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR + "/files", mock(LockFactory.class)), client, ftpReadCommand);

    for (int i = 0; i < 9; i++) {
      String newPath = fullPath.substring(0, fullPath.length() - 4) + i;
      newPath = newPath.concat(".txt");
      testHarness.write(newPath, fileContent);
    }

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/New.txt");
    String[] files = testHarness.getFileList(TEMP_DIRECTORY);
    assertThat(file, is(nullValue()));
    assertThat(files.length, is(10));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(anyString());
  }

}
