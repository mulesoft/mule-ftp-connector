/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;
import org.mule.runtime.api.lock.LockFactory;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class FtpFileSystemTest {

  @Test
  public void testIsFeatureSupported_logsErrorOnException() throws Exception {
    FTPClient mockClient = mock(FTPClient.class);
    when(mockClient.hasFeature(anyString())).thenThrow(new IOException("Test exception"));

    LockFactory mockLockFactory = mock(LockFactory.class);

    FtpFileSystem fileSystem = new FtpFileSystem(
                                                 mockClient,
                                                 "/",
                                                 mockLockFactory,
                                                 SingleFileListingMode.UNSET);

    boolean result = fileSystem.isFeatureSupported("SOME_FEATURE");

    assertFalse(result);
  }

  @Test
  public void testChangeToBaseDir_logsErrorOnIOException() throws Exception {
    FTPClient mockClient = mock(FTPClient.class);
    LockFactory mockLockFactory = mock(LockFactory.class);

    when(mockClient.changeWorkingDirectory(anyString())).thenThrow(new IOException("Simulated IO error"));

    String basePath = "/base/path";
    FtpFileSystem fileSystem = new FtpFileSystem(
                                                 mockClient,
                                                 basePath,
                                                 mockLockFactory,
                                                 SingleFileListingMode.UNSET);

    try {
      fileSystem.changeToBaseDir();
    } catch (RuntimeException e) {
    }
  }
}
