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

@RunWith(MockitoJUnitRunner.class)
public class FtpAbstractConnectionProviderTest {

  @Mock
  private LockFactory lockFactory;

  @Mock
  private FTPClient ftpClient;

  @Mock
  private FtpFileSystem ftpFileSystem;

  private TestFtpConnectionProvider provider;
  private FtpConnectionSettings connectionSettings;
  private TimeoutSettings timeoutSettings;

  @Before
  public void setUp() {
    provider = new TestFtpConnectionProvider();
    connectionSettings = new FtpConnectionSettings();
    connectionSettings.setHost("localhost");

    timeoutSettings = new TimeoutSettings();
    timeoutSettings.setConnectionTimeout(5000);
    timeoutSettings.setConnectionTimeoutUnit(TimeUnit.MILLISECONDS);
    timeoutSettings.setResponseTimeout(5000);
    timeoutSettings.setResponseTimeoutUnit(TimeUnit.MILLISECONDS);

    provider.setConnectionSettings(connectionSettings);
    provider.setTimeoutSettings(timeoutSettings);
    provider.setLockFactory(lockFactory);
  }

  @Test(expected = FTPConnectionException.class)
  public void testConnectInvalidCredentials() throws Exception {
    when(ftpClient.getReplyCode()).thenReturn(530);

    provider.connect();
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
  public void testOnBorrow() {
    provider.setTransferMode(FtpTransferMode.BINARY);
    provider.setPassive(true);

    provider.onBorrow(ftpFileSystem);
    verify(ftpFileSystem).setTransferMode(FtpTransferMode.BINARY);
    verify(ftpFileSystem).setPassiveMode(true);
  }

  @Test
  public void testTimeoutSettings() {
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    newTimeoutSettings.setConnectionTimeout(1000);
    newTimeoutSettings.setConnectionTimeoutUnit(TimeUnit.SECONDS);
    newTimeoutSettings.setResponseTimeout(2000);
    newTimeoutSettings.setResponseTimeoutUnit(TimeUnit.SECONDS);

    provider.setTimeoutSettings(newTimeoutSettings);

    assertEquals(Integer.valueOf(1000), provider.getConnectionTimeout());
    assertEquals(TimeUnit.SECONDS, provider.getConnectionTimeoutUnit());
    assertEquals(Integer.valueOf(2000), provider.getResponseTimeout());
    assertEquals(TimeUnit.SECONDS, provider.getResponseTimeoutUnit());
  }

  @Test
  public void testDisconnect() {
    provider.disconnect(ftpFileSystem);
    verify(ftpFileSystem).disconnect();
  }

  @Test
  public void testConnectionTimeoutGettersAndSetters() {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    provider.setTimeoutSettings(newTimeoutSettings);

    // Test setting and getting connection timeout
    Integer expectedTimeout = 3000;
    newTimeoutSettings.setConnectionTimeout(expectedTimeout);
    provider.setConnectionTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getConnectionTimeout());

    // Test setting and getting connection timeout unit
    TimeUnit expectedUnit = TimeUnit.SECONDS;
    newTimeoutSettings.setConnectionTimeoutUnit(expectedUnit);
    provider.setConnectionTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getConnectionTimeoutUnit());

    // Test null values
    newTimeoutSettings.setConnectionTimeout(null);
    provider.setConnectionTimeout(null);
    assertNull(provider.getConnectionTimeout());

    newTimeoutSettings.setConnectionTimeoutUnit(null);
    provider.setConnectionTimeoutUnit(null);
    assertNull(provider.getConnectionTimeoutUnit());
  }

  @Test
  public void testResponseTimeoutGettersAndSetters() {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    provider.setTimeoutSettings(newTimeoutSettings);

    // Test setting and getting response timeout
    Integer expectedTimeout = 4000;
    newTimeoutSettings.setResponseTimeout(expectedTimeout);
    provider.setResponseTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getResponseTimeout());

    // Test setting and getting response timeout unit
    TimeUnit expectedUnit = TimeUnit.MINUTES;
    newTimeoutSettings.setResponseTimeoutUnit(expectedUnit);
    provider.setResponseTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getResponseTimeoutUnit());

    // Test null values
    newTimeoutSettings.setResponseTimeout(null);
    provider.setResponseTimeout(null);
    assertNull(provider.getResponseTimeout());

    newTimeoutSettings.setResponseTimeoutUnit(null);
    provider.setResponseTimeoutUnit(null);
    assertNull(provider.getResponseTimeoutUnit());
  }

  @Test
  public void testHandleClientReplyCode() throws Exception {
    when(ftpClient.getReplyCode()).thenReturn(421);

    try {
      provider.connect();
      fail("Expected FTPConnectionException");
    } catch (FTPConnectionException e) {
      assertTrue(e.getMessage().contains("Service is unavailable"));
    }
  }

  // Test implementation of abstract class
  private class TestFtpConnectionProvider extends FtpAbstractConnectionProvider {

    private FtpConnectionSettings connectionSettings;
    private TimeoutSettings timeoutSettings;
    private LockFactory lockFactory;
    private String workingDir;
    private FtpTransferMode transferMode;
    private boolean passive;
    private String controlEncoding = "ISO-8859-1";

    @Override
    protected FTPClient createClient() {
      return ftpClient;
    }

    @Override
    public void initialise() throws InitialisationException {
      // No initialization needed for tests
    }

    @Override
    public String getWorkingDir() {
      return workingDir;
    }

    @Override
    public void onBorrow(FtpFileSystem connection) {
      connection.setTransferMode(transferMode);
      connection.setResponseTimeout(getResponseTimeout(), getResponseTimeoutUnit());
      connection.setPassiveMode(passive);
    }

    @Override
    protected Integer getConnectionTimeout() {
      return timeoutSettings != null ? timeoutSettings.getConnectionTimeout() : null;
    }

    @Override
    protected TimeUnit getConnectionTimeoutUnit() {
      return timeoutSettings != null ? timeoutSettings.getConnectionTimeoutUnit() : null;
    }

    @Override
    protected Integer getResponseTimeout() {
      return timeoutSettings != null ? timeoutSettings.getResponseTimeout() : null;
    }

    @Override
    protected TimeUnit getResponseTimeoutUnit() {
      return timeoutSettings != null ? timeoutSettings.getResponseTimeoutUnit() : null;
    }

    @Override
    protected ConnectionException handleClientReplyCode(int replyCode, Throwable cause) {
      switch (replyCode) {
        case 501:
        case 530:
          return new FTPConnectionException(getErrorMessage(replyCode, "User cannot log in"),
                                            INVALID_CREDENTIALS);
        case 421:
          return new FTPConnectionException(getErrorMessage(replyCode, "Service is unavailable"),
                                            SERVICE_NOT_AVAILABLE);
      }
      if (cause != null) {
        return new FTPConnectionException(getErrorMessage(connectionSettings,
                                                          String.format("Error code: '%d'", replyCode)),
                                          cause, CONNECTIVITY);
      }
      return new FTPConnectionException(getErrorMessage(connectionSettings,
                                                        String.format("Error code: '%d'", replyCode)));
    }

    private String getErrorMessage(FtpConnectionSettings settings, String message) {
      return String.format("Could not establish FTP connection with host: '%s' at port: '%d' - %s",
                           settings.getHost(), settings.getPort(), message);
    }

    @Override
    protected String getErrorMessage(int replyCode, String message) {
      return String.format("Could not establish FTP connection with host: '%s' at port: '%d' - Error code: %d - %s",
                           connectionSettings.getHost(), connectionSettings.getPort(), replyCode, message);
    }

    public void setConnectionSettings(FtpConnectionSettings connectionSettings) {
      this.connectionSettings = connectionSettings;
    }

    public void setTimeoutSettings(TimeoutSettings timeoutSettings) {
      this.timeoutSettings = timeoutSettings;
    }

    public void setLockFactory(LockFactory lockFactory) {
      this.lockFactory = lockFactory;
    }

    public void setWorkingDir(String workingDir) {
      this.workingDir = workingDir;
    }

    public void setTransferMode(FtpTransferMode transferMode) {
      this.transferMode = transferMode;
    }

    public void setPassive(boolean passive) {
      this.passive = passive;
    }
  }
}
