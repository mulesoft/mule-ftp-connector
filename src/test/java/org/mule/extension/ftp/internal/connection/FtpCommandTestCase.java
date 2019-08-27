/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.apache.commons.net.ftp.FTPClient;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.DefaultFtpTestHarness.FTP_PASSWORD;
import static org.mule.extension.ftp.DefaultFtpTestHarness.FTP_USER;
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
    String dirPath = "/" + WORKING_DIR + "/" + TEMP_DIRECTORY;
    String filePath = dirPath + "/NewFile.txt";
    testHarness.write(filePath, "File Content.");

    FTPClient client = spy(FTPClient.class);
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);
    when(client.mlistDir()).thenThrow(new IOException());

    ftpWriteCommand = new FtpWriteCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);

    assertThat(ftpWriteCommand.getFile(TEMP_DIRECTORY + "/1_file.txt"), is(nullValue()));
  }

  @Test
  public void readFileFromServerThatDoesNotSupportMListCommand() throws Exception {

    testHarness.makeDir(TEMP_DIRECTORY);

    String dirPath = "/" + WORKING_DIR + "/" + TEMP_DIRECTORY;
    String filePath = dirPath + "/NewFile.txt";
    testHarness.write(filePath, "File Content.");

    FTPClient client = spy(FTPClient.class);
    client.setDefaultTimeout(5000);
    client.connect("localhost", testHarness.getServerPort());
    client.login(FTP_USER, FTP_PASSWORD);
    when(client.mlistDir()).thenThrow(new IOException());

    ftpReadCommand = new FtpReadCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client);
    ftpListCommand = new FtpListCommand(new FtpFileSystem(client, WORKING_DIR, mock(LockFactory.class)), client, ftpReadCommand);

    Predicate matcher = spy(Predicate.class);
    when(matcher.test(any())).thenReturn(false);

    ftpListCommand.list(mock(FileConnectorConfig.class), dirPath, false, matcher, 0L);
    verify(client, times(1)).initiateListParsing();
    verify(matcher, times(1)).test(any());
  }

}
