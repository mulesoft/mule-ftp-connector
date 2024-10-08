/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.lock;

import java.nio.file.Path;

/**
 * Use to generate a lock on a file referenced by a path.
 *
 * @since 1.0
 */
public interface PathLock extends Lock {

  /**
   * @return The path to the locked file
   */
  Path getPath();

}
