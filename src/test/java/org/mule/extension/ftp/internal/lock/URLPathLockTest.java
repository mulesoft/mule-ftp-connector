/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.lock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.runtime.api.lock.LockFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;

@RunWith(MockitoJUnitRunner.class)
public class URLPathLockTest {

  private URL url;

  @Mock
  private LockFactory lockFactory;

  @Mock
  private Lock lock;

  private URLPathLock urlPathLock;

  @Before
  public void setUp() throws Exception {
    url = new URL("ftp://localhost/test.txt");
    when(lockFactory.createLock("ftp://localhost/test.txt")).thenReturn(lock);
    urlPathLock = new URLPathLock(url, lockFactory);
  }

  @Test
  public void isLockedWhenOwned() {
    when(lock.tryLock()).thenReturn(true);
    urlPathLock.tryLock();
    assertThat(urlPathLock.isLocked(), is(true));
  }

  @Test
  public void isLockedWhenNotOwned() {
    when(lock.tryLock()).thenReturn(false);
    assertThat(urlPathLock.isLocked(), is(true));
  }

  @Test
  public void isNotLockedWhenCanAcquire() {
    when(lock.tryLock()).thenReturn(true);
    assertThat(urlPathLock.isLocked(), is(false));
  }

  @Test
  public void getPathSuccess() throws Exception {
    URL fileUrl = new URL("file:///test.txt");
    URLPathLock fileLock = new URLPathLock(fileUrl, lockFactory);
    Path path = fileLock.getPath();
    assertThat(path.toString(), is("/test.txt"));
  }

  @Test
  public void getPathFailure() throws Exception {
    URL invalidUrl = new URL("ftp://localhost/test file.txt");
    URLPathLock invalidLock = new URLPathLock(invalidUrl, lockFactory);

    try {
      invalidLock.getPath();
      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertThat(e.getCause(), instanceOf(URISyntaxException.class));
    }
  }

  @Test
  public void getUriSuccess() throws Exception {
    URI expectedUri = new URI("ftp://localhost/test.txt");
    assertThat(urlPathLock.getUri(), is(expectedUri));
  }

  @Test
  public void getUriFailure() throws Exception {
    URL invalidUrl = new URL("ftp://localhost/test file.txt");
    URLPathLock invalidLock = new URLPathLock(invalidUrl, lockFactory);

    try {
      invalidLock.getUri();
      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertThat(e.getCause(), instanceOf(URISyntaxException.class));
    }
  }
}
