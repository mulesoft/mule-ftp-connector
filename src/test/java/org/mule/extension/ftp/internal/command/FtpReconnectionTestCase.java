/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.hamcrest.core.Is.isA;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.nio.file.Path;

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

  private FtpDummyCommand command = new FtpDummyCommand();

  @Rule
  public ExpectedException ftpConnectionThrown = ExpectedException.none();

  @Test
  @Description("When the FTP connection closes, a FTPConnectionClosedException is raised. " +
      "This should be treated as a ConnectionException")
  public void testReconnectionFromFTPConnectionClosed() {
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

    public FtpDummyCommand() {
      super(null, new FTPClient() {

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
    protected Path getBasePath(FileSystem fileSystem) {
      return getTempDirectory().toPath();
    }
  }
}
