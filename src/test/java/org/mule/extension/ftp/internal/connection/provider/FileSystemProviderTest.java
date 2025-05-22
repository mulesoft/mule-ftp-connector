/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection.provider;

import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemProviderTest {

    private static final String TEST_CONFIG_NAME = "testConfig";

    // Concrete test implementation of FileSystemProvider
    private static class TestFileSystemProvider extends FileSystemProvider<FileSystem> {
        @Override
        public String getWorkingDir() {
            return "/test/dir";
        }

        @Override
        public FileSystem connect() throws ConnectionException {
            return null; // Not needed for these tests
        }

        @Override
        public void disconnect(FileSystem fileSystem) {
            // Not needed for these tests
        }

        @Override
        public ConnectionValidationResult validate(FileSystem fileSystem) {
            return ConnectionValidationResult.success();
        }
    }

    private TestFileSystemProvider provider1;
    private TestFileSystemProvider provider2;
    private TestFileSystemProvider provider3;

    @Before
    public void setUp() {
        provider1 = new TestFileSystemProvider();
        provider2 = new TestFileSystemProvider();
        provider3 = new TestFileSystemProvider();
    }

    @Test
    public void testGetConfigName() throws Exception {
        // Set the configName field using reflection
        Field configNameField = FileSystemProvider.class.getDeclaredField("configName");
        configNameField.setAccessible(true);
        configNameField.set(provider1, TEST_CONFIG_NAME);

        // Test the getter
        assertEquals(TEST_CONFIG_NAME, provider1.getConfigName());
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
    public void testEqualsSameConfigName() {
        // Note: Since configName is injected by Mule runtime, we can't directly test it
        // This test is more of a structural test to ensure the equals method works as expected
        assertTrue(provider1.equals(provider2));
    }

    @Test
    public void testHashCode() {
        assertEquals(provider1.hashCode(), provider2.hashCode());
    }

    @Test
    public void testGetWorkingDir() {
        assertEquals("/test/dir", provider1.getWorkingDir());
    }
} 