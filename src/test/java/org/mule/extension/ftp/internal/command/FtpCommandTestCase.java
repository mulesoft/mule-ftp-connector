/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.DefaultFtpTestHarness;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.extension.ftp.internal.connection.SingleFileListingMode;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

import static org.apache.commons.net.ftp.FTPCmd.MLST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
import static org.mule.extension.ftp.api.FileTestHarness.WORKING_DIR;

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
  private SingleFileListingMode singleFileListingMode = SingleFileListingMode.SUPPORTED;

  @Before
  public void setUp() throws Exception {
    testHarness.makeDir(TEMP_DIRECTORY);
    testHarness.write(fullPath, fileContent);

    client = spy(FTPClient.class);
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);
  }

  @After
  public void tearDown() throws Exception {
    client.disconnect();
  }

  @Test
  public void listRecentlyCreatedDirectory() throws Exception {
    ftpWriteCommand =
        new FtpWriteCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    assertThat(ftpWriteCommand.getFile(TEMP_DIRECTORY), is(notNullValue()));
  }

  @Test
  public void getFileAttributesFromServerThatDoesNotSupportMLSTCommandWithMalformedServerReplyExceptionResponse()
      throws Exception {
    doThrow(new MalformedServerReplyException()).when(client).mlistFile(any());

    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(anyString());
  }

  @Test
  public void getFileAttributesFromServerThatDoesNotSupportMLSTCommandWithNullResponse()
      throws Exception {
    doReturn(null).when(client).mlistFile(any());

    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    FtpFileAttributes file = ftpReadCommand.getFile(fullPath);

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(1)).initiateListParsing(anyString());
  }

  @Test
  public void getFileAttributesFromServerThatSupportsMLSTCommand()
      throws Exception {
    // Although the server we use supports MLST by default, it has a bug and returns a non-complaint response to the
    // command. See https://issues.apache.org/jira/browse/FTPSERVER-480. Therefore we must mock its behaviour.
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName(fileName);
    doReturn(ftpFile).when(client).mlistFile(any());

    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    FtpFileAttributes file = ftpReadCommand.getFile(fullPath);

    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
    verify(client, times(0)).initiateListParsing(anyString());
  }

  @Test
  public void listDirectoryFromServerThatDoesNotSupportMLSDCommandWithNegativeCompletion() throws Exception {
    doReturn(new FTPFile[0]).when(client).mlistDir();
    doReturn(522).doCallRealMethod().when(client).getReplyCode();

    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client,
                           ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, times(1)).mlistDir();
    verify(client, times(1)).initiateListParsing(anyString());
  }

  @Test
  public void listDirectoryFromServerThatDoesNotSupportMLSDCommandWithMalformedServerReplyException() throws Exception {
    doThrow(new MalformedServerReplyException()).when(client).mlistDir();

    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client,
                           ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, atLeastOnce()).hasFeature(MLST.getCommand());
    verify(client, times(1)).mlistDir();
    verify(client, times(1)).initiateListParsing(anyString());
  }

  @Test
  public void listDirectoryFromServerThatDoesNotListMLSDasFeatureFallsBackToListCommand() throws Exception {
    when(client.hasFeature(MLST.getCommand())).thenReturn(false);

    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client,
                           ftpReadCommand);

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
    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client,
                           ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    List<Result<InputStream, FtpFileAttributes>> files =
        ftpListCommand.list(mock(FileConnectorConfig.class), "/" + WORKING_DIR, false, matcher, 0L);
    assertThat(files.size(), is(1));
    assertThat(files.get(0).getAttributes().get().getName(), is(TEMP_DIRECTORY));
    verify(client, atLeastOnce()).hasFeature(MLST.getCommand());
    verify(client, times(1)).mlistDir();
    verify(client, times(0)).initiateListParsing(anyString());
  }

  @Test
  public void testThatGetFileFromParentDirectoryReturnsFileEvenIfThereIsNotMLSTCommand() throws Exception {
    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);
    when(client.mlistFile(anyString())).thenThrow(new MalformedServerReplyException());

    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(2)).mlistFile(any());
  }

  @Test
  public void testThatGetFileFromParentDirectory2ReturnsFileEvenIfThereIsNotMLSTCommand() throws Exception {
    ftpReadCommand =
        new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class), singleFileListingMode), client);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(true);

    FtpFileAttributes file = ftpReadCommand.getFile(TEMP_DIRECTORY + "/NewFile.txt");
    assertThat(file, is(notNullValue()));
    assertThat(file.getName(), is(fileName));
    verify(client, times(1)).mlistFile(any());
  }

}
