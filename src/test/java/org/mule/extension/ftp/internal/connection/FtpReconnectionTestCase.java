/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.hamcrest.core.Is.isA;
import static org.mockito.Matchers.anyString;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.ftp.internal.command.FtpCommand;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@SmallTest
@Feature("Reconnection from FTP exception")
public class FtpReconnectionTestCase extends AbstractMuleTestCase {

  private FtpDummyCommand command;

  @Rule
  public ExpectedException ftpConnectionThrown = ExpectedException.none();

  @Test
  @Description("When the FTP connection closes, a FTPConnectionClosedException is raised. " +
      "This should be treated as a ConnectionException")
  public void testReconnectionFromFTPConnectionClosed() {
    FtpFileSystem ftpFileSystemMock = Mockito.mock(FtpFileSystem.class);
    when(ftpFileSystemMock.isFeatureSupported(anyString())).thenReturn(true);
    command = new FtpDummyCommand(ftpFileSystemMock);
    ftpConnectionThrown.expectCause(isA(ConnectionException.class));
    ftpConnectionThrown.expectCause(new TypeSafeMatcher<Throwable>() {

      @Override
      protected boolean matchesSafely(Throwable throwable) {
        return isA(FTPConnectionClosedException.class).matches(throwable.getCause());
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {}
    });

    command.getFile("");
  }

  private class FtpDummyCommand extends FtpCommand {

    public FtpDummyCommand(FtpFileSystem ftpFileSystem) {
      super(ftpFileSystem, new FTPClient() {

        @Override
        public FTPFile mlistFile(String pathname) throws IOException {
          throw new FTPConnectionClosedException();
        }

        @Override
        public int getReplyCode() {
          return -1;
        }
      });
    }

    @Override
    protected URI getBasePath(FileSystem fileSystem) {
      return createUri(getTempDirectory().toString());
    }
  }
}
