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

  private FtpConnectionProvider provider;
  private FtpConnectionSettings connectionSettings;
  private TimeoutSettings timeoutSettings;

  @Before
  public void setUp() throws Exception {
    provider = new FtpConnectionProvider() {

      @Override
      protected FTPClient createClient() {
        return ftpClient;
      }
    };
    connectionSettings = new FtpConnectionSettings();
    connectionSettings.setHost("localhost");

    timeoutSettings = new TimeoutSettings();
    timeoutSettings.setConnectionTimeout(5000);
    timeoutSettings.setConnectionTimeoutUnit(TimeUnit.MILLISECONDS);
    timeoutSettings.setResponseTimeout(5000);
    timeoutSettings.setResponseTimeoutUnit(TimeUnit.MILLISECONDS);

    // Use reflection to set private fields
    java.lang.reflect.Field connectionSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("connectionSettings");
    connectionSettingsField.setAccessible(true);
    connectionSettingsField.set(provider, connectionSettings);

    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, timeoutSettings);

    java.lang.reflect.Field lockFactoryField = FtpAbstractConnectionProvider.class.getDeclaredField("lockFactory");
    lockFactoryField.setAccessible(true);
    lockFactoryField.set(provider, lockFactory);

    java.lang.reflect.Field workingDirField = FtpAbstractConnectionProvider.class.getDeclaredField("workingDir");
    workingDirField.setAccessible(true);
    workingDirField.set(provider, "/test/dir");

    java.lang.reflect.Field transferModeField = FtpAbstractConnectionProvider.class.getDeclaredField("transferMode");
    transferModeField.setAccessible(true);
    transferModeField.set(provider, FtpTransferMode.BINARY);

    java.lang.reflect.Field passiveField = FtpAbstractConnectionProvider.class.getDeclaredField("passive");
    passiveField.setAccessible(true);
    passiveField.set(provider, true);
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
  public void testGetWorkingDir() throws Exception {
    String workingDir = "/test/dir";
    java.lang.reflect.Field workingDirField = FtpAbstractConnectionProvider.class.getDeclaredField("workingDir");
    workingDirField.setAccessible(true);
    workingDirField.set(provider, workingDir);
    assertEquals(workingDir, provider.getWorkingDir());
  }

  @Test
  public void testOnBorrow() throws Exception {
    java.lang.reflect.Field transferModeField = FtpAbstractConnectionProvider.class.getDeclaredField("transferMode");
    transferModeField.setAccessible(true);
    transferModeField.set(provider, FtpTransferMode.BINARY);

    java.lang.reflect.Field passiveField = FtpAbstractConnectionProvider.class.getDeclaredField("passive");
    passiveField.setAccessible(true);
    passiveField.set(provider, true);

    provider.onBorrow(ftpFileSystem);
    verify(ftpFileSystem).setTransferMode(FtpTransferMode.BINARY);
    verify(ftpFileSystem).setPassiveMode(true);
  }

  @Test
  public void testTimeoutSettings() throws Exception {
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    newTimeoutSettings.setConnectionTimeout(1000);
    newTimeoutSettings.setConnectionTimeoutUnit(TimeUnit.SECONDS);
    newTimeoutSettings.setResponseTimeout(2000);
    newTimeoutSettings.setResponseTimeoutUnit(TimeUnit.SECONDS);

    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

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
  public void testConnectionTimeoutGettersAndSetters() throws Exception {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

    // Test setting and getting connection timeout
    Integer expectedTimeout = 3000;
    newTimeoutSettings.setConnectionTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getConnectionTimeout());

    // Test setting and getting connection timeout unit
    TimeUnit expectedUnit = TimeUnit.SECONDS;
    newTimeoutSettings.setConnectionTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getConnectionTimeoutUnit());

    // Test null values
    newTimeoutSettings.setConnectionTimeout(null);
    assertNull(provider.getConnectionTimeout());

    newTimeoutSettings.setConnectionTimeoutUnit(null);
    assertNull(provider.getConnectionTimeoutUnit());
  }

  @Test
  public void testResponseTimeoutGettersAndSetters() throws Exception {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

    // Test setting and getting response timeout
    Integer expectedTimeout = 4000;
    newTimeoutSettings.setResponseTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getResponseTimeout());

    // Test setting and getting response timeout unit
    TimeUnit expectedUnit = TimeUnit.MINUTES;
    newTimeoutSettings.setResponseTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getResponseTimeoutUnit());

    // Test null values
    newTimeoutSettings.setResponseTimeout(null);
    assertNull(provider.getResponseTimeout());

    newTimeoutSettings.setResponseTimeoutUnit(null);
    assertNull(provider.getResponseTimeoutUnit());
  }

  @Test
  public void testHandleClientReplyCode() throws Exception {
    // Set up mock behavior
    when(ftpClient.getReplyCode()).thenReturn(421);
    doNothing().when(ftpClient).connect(anyString(), anyInt());

    try {
      provider.connect();
      fail("Expected FTPConnectionException");
    } catch (FTPConnectionException e) {
      assertTrue(e.getMessage().contains("Service is unavailable"));
    }
  }

  @Test
  public void testSetConnectionTimeout() throws Exception {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

    // Test setting connection timeout
    Integer expectedTimeout = 5000;
    provider.setConnectionTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getConnectionTimeout());
  }

  @Test
  public void testSetConnectionTimeoutUnit() throws Exception {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

    // Test setting connection timeout unit
    TimeUnit expectedUnit = TimeUnit.SECONDS;
    provider.setConnectionTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getConnectionTimeoutUnit());
  }

  @Test
  public void testSetResponseTimeout() throws Exception {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

    // Test setting response timeout
    Integer expectedTimeout = 3000;
    provider.setResponseTimeout(expectedTimeout);
    assertEquals(expectedTimeout, provider.getResponseTimeout());
  }

  @Test
  public void testSetResponseTimeoutUnit() throws Exception {
    // Create new timeout settings to avoid interference from setUp()
    TimeoutSettings newTimeoutSettings = new TimeoutSettings();
    java.lang.reflect.Field timeoutSettingsField = FtpAbstractConnectionProvider.class.getDeclaredField("timeoutSettings");
    timeoutSettingsField.setAccessible(true);
    timeoutSettingsField.set(provider, newTimeoutSettings);

    // Test setting response timeout unit
    TimeUnit expectedUnit = TimeUnit.MINUTES;
    provider.setResponseTimeoutUnit(expectedUnit);
    assertEquals(expectedUnit, provider.getResponseTimeoutUnit());
  }
}
