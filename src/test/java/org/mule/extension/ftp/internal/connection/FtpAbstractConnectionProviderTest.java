/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mule.extension.ftp.api.FileError.*;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.extension.ftp.api.ftp.FtpTransferMode;
import org.mule.extension.ftp.internal.TimeoutSettings;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.lifecycle.InitialisationException;

import java.util.concurrent.TimeUnit;
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

class TestFtpConnectionProvider extends FtpConnectionProvider {

  private final FTPClient ftpClient;
  private String workingDir;
  private FtpTransferMode transferMode;
  private boolean passive;

  public TestFtpConnectionProvider(FTPClient ftpClient) {
    this.ftpClient = ftpClient;
  }

  @Override
  protected FTPClient createClient() {
    return ftpClient;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }

  @Override
  public String getWorkingDir() {
    return workingDir;
  }
}


@RunWith(MockitoJUnitRunner.class)
public class FtpAbstractConnectionProviderTest {

  @Mock
  private FTPClient ftpClient;

  @Mock
  private FtpFileSystem ftpFileSystem;

  private TestFtpConnectionProvider provider;
  private FtpConnectionSettings connectionSettings;
  private TimeoutSettings timeoutSettings;

  @Before
  public void setUp() throws Exception {
    provider = new TestFtpConnectionProvider(ftpClient);
    connectionSettings = new FtpConnectionSettings();
    connectionSettings.setHost("localhost");

    timeoutSettings = new TimeoutSettings();
    timeoutSettings.setConnectionTimeout(5000);
    timeoutSettings.setConnectionTimeoutUnit(TimeUnit.MILLISECONDS);
    timeoutSettings.setResponseTimeout(5000);
    timeoutSettings.setResponseTimeoutUnit(TimeUnit.MILLISECONDS);
  }

  @Test
  public void testValidateConnection() {
    when(ftpFileSystem.validateConnection()).thenReturn(ConnectionValidationResult.success());

    ConnectionValidationResult result = provider.validate(ftpFileSystem);
    assertTrue(result.isValid());
  }

  @Test
  public void testGetWorkingDir() {
    String workingDir = "/test/dir";
    provider.setWorkingDir(workingDir);
    assertEquals(workingDir, provider.getWorkingDir());
  }

  @Test
  public void testDisconnect() {
    provider.disconnect(ftpFileSystem);
    verify(ftpFileSystem).disconnect();
  }

  @Test
  public void testConnectionTimeoutGettersAndSetters() {
    // Test setting and getting connection timeout
    Integer expectedTimeout = 3000;
    provider.setConnectionTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getConnectionTimeout());

    // Test setting and getting connection timeout unit
    TimeUnit expectedUnit = TimeUnit.SECONDS;
    provider.setConnectionTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getConnectionTimeoutUnit());
  }

  @Test
  public void testResponseTimeoutGettersAndSetters() {
    // Test setting and getting response timeout
    Integer expectedTimeout = 4000;
    provider.setResponseTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getResponseTimeout());

    // Test setting and getting response timeout unit
    TimeUnit expectedUnit = TimeUnit.MINUTES;
    provider.setResponseTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getResponseTimeoutUnit());
  }

  @Test
  public void testSetConnectionTimeout() {
    // Test setting connection timeout
    Integer expectedTimeout = 5000;
    provider.setConnectionTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getConnectionTimeout());
  }

  @Test
  public void testSetConnectionTimeoutUnit() {
    // Test setting connection timeout unit
    TimeUnit expectedUnit = TimeUnit.SECONDS;
    provider.setConnectionTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getConnectionTimeoutUnit());
  }

  @Test
  public void testSetResponseTimeout() {
    // Test setting response timeout
    Integer expectedTimeout = 3000;
    provider.setResponseTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getResponseTimeout());
  }

  @Test
  public void testSetResponseTimeoutUnit() {
    // Test setting response timeout unit
    TimeUnit expectedUnit = TimeUnit.MINUTES;
    provider.setResponseTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getResponseTimeoutUnit());
  }

  @Test
  public void testSetupWireLogging() {
    StringBuilder loggedMessages = new StringBuilder();
    Consumer<String> operation = message -> loggedMessages.append(message);
    provider.setupWireLogging(ftpClient, operation);

    verify(ftpClient).addProtocolCommandListener(any());
  }
}
