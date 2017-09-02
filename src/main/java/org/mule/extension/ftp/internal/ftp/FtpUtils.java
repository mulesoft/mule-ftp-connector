/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp;

import static java.lang.Thread.currentThread;
import org.apache.commons.io.FilenameUtils;

import java.net.URL;
import java.nio.file.Path;

/**
 * Utility class for normalizing FTP paths
 *
 * @since 1.0
 */
public class FtpUtils {

  private FtpUtils() {}

  /**
   * @param path to be normalized
   * @return a {@link String} representing the path in the following format (using the unix path separator): "directory/subdirectory"
   */
  public static String normalizePath(String path) {
    path = path.length() > 2 && (path.charAt(1) == ':' || path.charAt(2) == ':') ? path.substring(path.indexOf(':') + 1) : path;
    return FilenameUtils.normalize(path, true);
  }

  public static String normalizePath(Path path) {
    return normalizePath(path.toString());
  }

  public static String resolvePath(String pathOrResourceName) {
    URL resource = currentThread().getContextClassLoader().getResource(pathOrResourceName);
    return resource != null ? resource.getPath() : pathOrResourceName;
  }
}
