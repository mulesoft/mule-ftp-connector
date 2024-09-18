/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.exception;

import org.mule.runtime.extension.api.exception.ModuleException;

import static org.mule.extension.ftp.api.FileError.FILE_LOCK;

/**
 * {@link ModuleException} for the cases in which a lock cannot be acquired over a file.
 * 
 * @since 1.0
 */
public final class FileLockedException extends ModuleException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public FileLockedException(String message) {
    super(message, FILE_LOCK);
  }
}

