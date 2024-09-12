/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.api.UriUtils.createUri;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import io.qameta.allure.Feature;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@Feature(FTP_EXTENSION)
@RunWith(MockitoJUnitRunner.class)
public class FtpFileAttributesTestCase {

  private static final String EXPECTED_FILENAME = "hello.txt";
  private static final String EXPECTED_PATH = "/root/" + EXPECTED_FILENAME;
  private static final long EXPECTED_SIZE = 100L;
  private static final boolean IS_DIRECTORY = false;
  private static final boolean IS_SYM_LINK = false;
  private static final boolean IS_FILE = true;

  @Mock
  private FTPFile ftpFile;

  private URI uri = createUri(EXPECTED_PATH);

  private LocalDateTime expectedTimesTamp;

  @Before
  public void setUp() {
    Calendar currentDate = Calendar.getInstance();
    expectedTimesTamp = LocalDateTime.ofInstant(currentDate.toInstant(), ZoneId.systemDefault());

    when(ftpFile.getTimestamp()).thenReturn(currentDate);
    doReturn(EXPECTED_FILENAME).when(ftpFile).getName();
    when(ftpFile.getSize()).thenReturn(EXPECTED_SIZE);
    when(ftpFile.isFile()).thenReturn(IS_FILE);
    when(ftpFile.isDirectory()).thenReturn(IS_DIRECTORY);
    when(ftpFile.isSymbolicLink()).thenReturn(IS_SYM_LINK);
  }

  @Test
  public void allAttributesPopulatedFromFile() {
    FtpFileAttributes ftpAttributes = getFtpFileAttributes();
    assertThat(ftpAttributes.getPath(), is(EXPECTED_PATH));
    assertThat(ftpAttributes.getName(), is(EXPECTED_FILENAME));
    assertThat(ftpAttributes.getSize(), is(EXPECTED_SIZE));
    assertThat(ftpAttributes.getTimestamp(), is(expectedTimesTamp));
    assertThat(ftpAttributes.isDirectory(), is(IS_DIRECTORY));
    assertThat(ftpAttributes.isRegularFile(), is(IS_FILE));
    assertThat(ftpAttributes.isSymbolicLink(), is(IS_SYM_LINK));
  }

  @Test
  public void missingTimestampFromFtpFile() {
    when(ftpFile.getTimestamp()).thenReturn(null);

    FtpFileAttributes ftpAttributes = getFtpFileAttributes();
    assertThat(ftpAttributes.getTimestamp(), is(nullValue()));
  }

  private FtpFileAttributes getFtpFileAttributes() {
    return new FtpFileAttributes(uri, ftpFile);
  }

}
