/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.operation;

import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.slf4j.Logger;

import java.net.URI;

import static org.mule.extension.ftp.internal.util.UriUtils.createUri;
import static org.mule.extension.ftp.internal.util.UriUtils.trimLastFragment;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Extension of {@link AbstractFileCommand} for local file systems which use {@link URI} to identify and manage
 * files and directories.
 *
 * @param <F> the generic type of the {@link FtpFileSystem} on which the operation is performed
 * @since 1.3.0
 */
public abstract class ExternalFileCommand<F extends FtpFileSystem> extends AbstractFileCommand<F, URI> {

  private static final Logger LOGGER = getLogger(ExternalFileCommand.class);

  /**
   * Creates a new instance
   *
   * @param ftpFileSystem the {@link FtpFileSystem} on which the operation is performed
   */
  protected ExternalFileCommand(F ftpFileSystem) {
    super(ftpFileSystem);
  }

  /**
   * {@inheritDoc}
   */
  protected String pathToString(URI uri) {
    return uri.getPath();
  }

  /**
   * {@inheritDoc}
   *
   * @return an absolute {@link URI}
   */
  protected URI getAbsolutePath(URI uri) {
    return uri;
  }

  /**
   * {@inheritDoc}
   *
   * @return the parent {@link URI}
   */
  protected URI getParent(URI uri) {
    return trimLastFragment(uri);
  }

  /**
   * {@inheritDoc}
   *
   * @return the resolved {@link URI}
   */
  protected URI resolvePath(URI baseUri, String filePath) {
    return createUri(baseUri.getPath(), filePath);
  }

}
