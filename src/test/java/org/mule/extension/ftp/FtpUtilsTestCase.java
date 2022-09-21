/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.extension.ftp.internal.FtpUtils.createUrl;
import static org.mule.extension.ftp.internal.FtpUtils.updatePathIfUnderRoot;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import io.qameta.allure.Feature;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpUtilsTestCase {

  private final static String PROTOCOL = "ftp";
  private final static String HOST = "127.0.0.1";
  private final static int PORT = 80;
  private final static String PATH = EMPTY;
  private final static String FILE_PATH_UNDER_ROOT = "/Test1.txt";
  private final static String FILE_NORMAL_PATH = "/Home/Test1.txt";

  private FTPClient client = mock(FTPClient.class);
  private InetAddress address = mock(InetAddress.class);

  @Test
  public void validUrl() throws MalformedURLException {
    when(address.getHostAddress()).thenReturn(HOST);
    when(client.getRemoteAddress()).thenReturn(address);
    when(client.getRemotePort()).thenReturn(PORT);
    URL url = createUrl(client, null);
    assertUrl(url);
  }

  @Test(expected = MalformedURLException.class)
  public void invalidPort() throws MalformedURLException {
    when(address.getHostAddress()).thenReturn(HOST);
    when(client.getRemoteAddress()).thenReturn(address);
    when(client.getRemotePort()).thenReturn(-2);
    createUrl(client, null);
  }

  @Test
  public void testUpdatePathIfUnderRoot() {
    String path1 = updatePathIfUnderRoot(FILE_PATH_UNDER_ROOT);
    assertEquals(FILE_PATH_UNDER_ROOT.substring(1), path1);

    String path2 = updatePathIfUnderRoot(FILE_NORMAL_PATH);
    assertEquals(FILE_NORMAL_PATH, path2);
  }

  private void assertUrl(URL url) {
    assertThat(url.getProtocol(), is(PROTOCOL));
    assertThat(url.getHost(), is(HOST));
    assertThat(url.getPort(), is(PORT));
    assertThat(url.getPath(), is(EMPTY));
  }
}
