/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.junit.ClassRule;
import org.junit.Test;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.ftp.DefaultFtpTestHarness;
import org.mule.extension.ftp.internal.command.FtpListCommand;
import org.mule.extension.ftp.internal.command.FtpReadCommand;
import org.mule.extension.ftp.internal.command.FtpWriteCommand;
import org.mule.runtime.api.lock.LockFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.DefaultFtpTestHarness.FTP_PASSWORD;
import static org.mule.extension.ftp.DefaultFtpTestHarness.FTP_USER;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_WORLD;
import static org.mule.test.extension.file.common.api.FileTestHarness.WORKING_DIR;

import java.io.IOException;
import java.util.function.Predicate;

public class FtpCommandTestCase {

  private static final String TEMP_DIRECTORY = "files";

  @ClassRule
  public static DefaultFtpTestHarness testHarness = new DefaultFtpTestHarness();

  private FtpWriteCommand ftpWriteCommand;
  private FtpListCommand ftpListCommand;
  private FtpReadCommand ftpReadCommand;

  @Test
  public void writeFileOnEmptyNewFolderWithCreateParentFoldersOnFalse() throws Exception {

    testHarness.makeDir(TEMP_DIRECTORY);

    FTPClient client;
    client = new FTPClient();
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);

    ftpWriteCommand = new FtpWriteCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);

    assertThat(ftpWriteCommand.getFile(TEMP_DIRECTORY), is(notNullValue()));
  }

  @Test
  public void testSimilarNames() throws Exception {

    testHarness.makeDir(TEMP_DIRECTORY);
    testHarness.write(TEMP_DIRECTORY + "/file.txt", HELLO_WORLD);

    FTPClient client;
    client = new FTPClient();
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);

    ftpWriteCommand = new FtpWriteCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);

    assertThat(ftpWriteCommand.getFile(TEMP_DIRECTORY + "/1_file.txt"), is(nullValue()));
  }

  @Test
  public void readFileFromServerThatDoesNotSupportMListCommand() throws Exception {

    String dirPath = "/" + TEMP_DIRECTORY;
    String filePath = dirPath + "/NewFile.txt";
    testHarness.makeDir(TEMP_DIRECTORY);
    testHarness.write(filePath, "File Content.");

    FTPClient client;
    client = new FTPClient();
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);

    FTPClient mockClient = mock(FTPClient.class);
    FTPListParseEngine mockEngine = mock(FTPListParseEngine.class);
    when(mockEngine.hasNext()).thenReturn(false);

    when(mockClient.mlistDir(anyString())).thenThrow(new IOException());
    when(mockClient.initiateListParsing(any())).thenReturn(mockEngine);
    when(mockClient.changeWorkingDirectory(anyString())).thenReturn(client.changeWorkingDirectory(dirPath));
    when(mockClient.mlistFile(anyString())).thenThrow(new IOException());
    when(mockClient.listFiles(anyString())).thenReturn(client.listFiles(dirPath));
    when(mockClient.listDirectories(anyString())).thenReturn(client.listDirectories("/" + WORKING_DIR));
    when(mockClient.printWorkingDirectory()).thenReturn("/");

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(mockClient, WORKING_DIR, mock(LockFactory.class)), mockClient);
    ftpListCommand =
        new FtpListCommand(new FtpFileSystem(mockClient, WORKING_DIR, mock(LockFactory.class)), mockClient, ftpReadCommand);

    assertThat(ftpListCommand.list(mock(FileConnectorConfig.class), filePath, false, mock(Predicate.class), 0L),
               is(notNullValue()));
  }

}
