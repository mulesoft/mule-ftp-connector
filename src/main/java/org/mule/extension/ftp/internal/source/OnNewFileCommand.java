/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.source;

import static java.lang.String.format;

import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.ftp.internal.command.FtpCommand;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * A {@link FtpCommand} which implements support functionality for {@link FtpDirectoryListener}
 *
 * @since 1.1
 */
public class OnNewFileCommand extends FtpCommand {

  OnNewFileCommand(FtpFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * Resolves the root path on which the listener needs to be created
   *
   * @param directory the path that the user configured on the listener
   * @return the resolved {@link URI} to listen on
   */
  public URI resolveRootUri(String directory) throws IOException {
    Optional<URI> uri = getUriToDirectory(directory);
    if (!uri.isPresent()) {
      throw new IllegalPathException(format("Path '%s' doesn't exist", directory));
    }
    return uri.get();
  }
}
