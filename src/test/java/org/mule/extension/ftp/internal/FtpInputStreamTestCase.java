/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.mule.extension.file.common.api.lock.UriLock;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FtpInputStreamTestCase {

  public static final String STREAM_CONTENT = "My stream content";

  @Mock
  private UriLock uriLock;

  @Mock
  private FtpInputStream.FtpFileInputStreamSupplier streamSupplier;

  @Mock
  private ConnectionHandler connectionHandler;

  @Mock
  private InputStream inputStream;

  @Mock
  private FtpFileSystem ftpFileSystem;

  @Before
  public void setUp() throws Exception {
    when(uriLock.isLocked()).thenReturn(true);
    doAnswer(invocation -> {
      when(uriLock.isLocked()).thenReturn(false);
      return null;
    }).when(uriLock).release();

    when(streamSupplier.getFtpFileSystem()).thenReturn(empty());
    when(streamSupplier.getConnectionHandler()).thenReturn(of(connectionHandler));
    when(streamSupplier.get()).thenReturn(new ByteArrayInputStream(STREAM_CONTENT.getBytes(UTF_8)));
  }

  @Test
  public void readLockReleasedOnContentConsumed() throws Exception {
    FtpInputStream inputStream = new ClassicFtpInputStream(streamSupplier, uriLock);

    verifyZeroInteractions(uriLock);
    assertThat(inputStream.isLocked(), is(true));
    verify(uriLock).isLocked();

    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8");

    verify(uriLock, times(1)).release();
    assertThat(inputStream.isLocked(), is(false));
    verify(connectionHandler).release();
  }

  @Test
  public void readLockReleasedOnEarlyClose() throws Exception {
    FtpInputStream inputStream = new ClassicFtpInputStream(streamSupplier, uriLock);

    verifyZeroInteractions(uriLock);
    assertThat(inputStream.isLocked(), is(true));
    verify(uriLock).isLocked();

    inputStream.close();

    verify(uriLock, times(1)).release();
    assertThat(inputStream.isLocked(), is(false));
  }

  @Test
  public void inputStreamClosesBeforeCompletePendingIsCalled() throws Exception {
    when(streamSupplier.get()).thenReturn(inputStream);
    when(inputStream.read()).thenReturn(5, 6, -1);
    when(streamSupplier.getFtpFileSystem()).thenReturn(of(ftpFileSystem));
    FtpInputStream ftpInputStream = new ClassicFtpInputStream(streamSupplier, uriLock);

    ftpInputStream.read();
    ftpInputStream.read();
    ftpInputStream.read();

    InOrder inOrder = inOrder(inputStream, ftpFileSystem);
    inOrder.verify(inputStream).close();
    inOrder.verify(ftpFileSystem).awaitCommandCompletion();
  }

}
