/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.mule.extension.ftp.internal.error.FileError.CANNOT_REACH;
import static org.mule.extension.ftp.internal.error.FileError.CONNECTION_TIMEOUT;
import static org.mule.extension.ftp.internal.error.FileError.INVALID_CREDENTIALS;
import static org.mule.extension.ftp.internal.error.FileError.SERVICE_NOT_AVAILABLE;
import static org.mule.extension.ftp.internal.error.FileError.UNKNOWN_HOST;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.junit.Ignore;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@Feature(FTP_EXTENSION)
@Ignore
@Story("Negative Connectivity Testing")
public class FtpNegativeConnectivityTestCase extends CommonFtpConnectorTestCase {

  private static final Matcher<Exception> ANYTHING =
      is(allOf(instanceOf(ConnectionException.class), hasCause(instanceOf(FTPConnectionException.class))));

  private TestConnectivityUtils utils;

  @Rule
  public SystemProperty rule = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Override
  protected String getConfigFile() {
    return "ftp-negative-connectivity-test.xml";
  }

  @Before
  public void setUp() {
    utils = new TestConnectivityUtils(registry);
  }

  @Test
  public void configInvalidCredentials() {
    utils.assertFailedConnection("ftpConfigInvalidCredentials", ANYTHING, is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void configConnectionTimeout() {
    utils.assertFailedConnection("ftpConfigConnectionTimeout", ANYTHING, is(errorType(CONNECTION_TIMEOUT)));
  }

  @Test
  public void connectionRefused() {
    utils.assertFailedConnection("ftpConfigConnectionRefused", ANYTHING, is(errorType(CANNOT_REACH)));
  }

  @Test
  public void configMissingCredentials() {
    utils.assertFailedConnection("ftpConfigMissingCredentials", ANYTHING, is(errorType(INVALID_CREDENTIALS)));
  }

  @Test
  public void configUnknownHost() {
    utils.assertFailedConnection("ftpConfigUnknownHost", ANYTHING, is(errorType(UNKNOWN_HOST)));
  }

  @Test
  public void ftpConfigServiceUnavailable() {
    utils.assertSuccessConnection("ftpConfigFirstConnection");
    utils.assertFailedConnection("ftpConfigServiceUnavailable", ANYTHING, is(errorType(SERVICE_NOT_AVAILABLE)));
  }
}
