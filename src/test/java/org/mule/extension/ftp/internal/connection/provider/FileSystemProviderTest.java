/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemProviderTest {

  private static final String TEST_CONFIG_NAME = "testConfig";
  private FileSystemProvider provider1;
  private FileSystemProvider provider2;

  private static class TestFileSystemProvider extends FileSystemProvider<FileSystem> {

    @Override
    public FileSystem connect() {
      return null;
    }

    @Override
    public void disconnect(FileSystem connection) {
    }

    @Override
    public ConnectionValidationResult validate(FileSystem connection) {
      return ConnectionValidationResult.success();
    }

    @Override
    public String getWorkingDir() {
      return "/test/dir";
    }
  }

  @Before
  public void setUp() {
    provider1 = new TestFileSystemProvider();
    provider2 = new TestFileSystemProvider();
  }

  @Test
  public void testEqualsSameInstance() {
    assertTrue(provider1.equals(provider1));
  }

  @Test
  public void testEqualsNull() {
    assertFalse(provider1.equals(null));
  }

  @Test
  public void testEqualsDifferentClass() {
    assertFalse(provider1.equals(new Object()));
  }

  @Test
  public void testHashCode() {
    assertEquals(provider1.hashCode(), provider2.hashCode());
  }


}
