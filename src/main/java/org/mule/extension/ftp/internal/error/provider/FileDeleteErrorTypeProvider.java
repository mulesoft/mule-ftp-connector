/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.error.provider;

import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.mule.extension.ftp.internal.error.FileError.ACCESS_DENIED;
import static org.mule.extension.ftp.internal.error.FileError.ILLEGAL_PATH;

/**
 * Errors that can be thrown in the {@link org.mule.extension.ftp.internal.FtpOperations#delete(FileSystem, String)} operation.
 * 
 * @since 1.0
 */
public class FileDeleteErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return unmodifiableSet(new HashSet<>(asList(ILLEGAL_PATH, ACCESS_DENIED)));
  }
}

