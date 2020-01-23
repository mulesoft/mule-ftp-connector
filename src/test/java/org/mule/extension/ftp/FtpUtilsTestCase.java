/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.extension.ftp.internal.FtpUtils.createUrl;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpUtilsTestCase {

  private final static String PROTOCOL = "ftp";
  private final static String HOST = "127.0.0.1";
  private final static int PORT = 80;
  private final static String PATH = EMPTY;

  private FTPClient client = mock(FTPClient.class);
  private InetAddress address = mock(InetAddress.class);

  @Test
  public void validUrl() throws MalformedURLException {
    URL url = createUrl(PROTOCOL, HOST, PORT, EMPTY);
    assertUrl(url);
  }

  @Test(expected = MalformedURLException.class)
  public void invalidProtocol() throws MalformedURLException {
    createUrl("FOO", HOST, PORT, PATH);
  }

  @Test(expected = MalformedURLException.class)
  @Description("")
  public void invalidProt() throws MalformedURLException {
    createUrl(PROTOCOL, HOST, -2, PATH);
  }

  @Test
  public void createValidUrlFromClient() throws MalformedURLException {
    when(address.getHostAddress()).thenReturn(HOST);
    when(client.getRemoteAddress()).thenReturn(address);
    when(client.getRemotePort()).thenReturn(PORT);
    URL url = createUrl(client, null);
    assertUrl(url);
  }

  @Test
  public void invalidUrlFromClient() throws MalformedURLException {
    when(address.getHostAddress()).thenReturn(HOST);
    when(client.getRemoteAddress()).thenReturn(address);
    when(client.getRemotePort()).thenReturn(PORT);
    URL url = createUrl(client, null);
    assertUrl(url);
  }

  private void assertUrl(URL url) {
    assertThat(url.getProtocol(), is(PROTOCOL));
    assertThat(url.getHost(), is(HOST));
    assertThat(url.getPort(), is(PORT));
    assertThat(url.getPath(), is(EMPTY));
  }
}
