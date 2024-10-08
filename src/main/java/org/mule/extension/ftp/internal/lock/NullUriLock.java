/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.lock;

import java.net.URI;

/**
 * Implementation of the Null Object design pattern for the {@link UriLock} interface
 *
 * @since 1.3.0
 */
public final class NullUriLock extends AbstractNullLock implements UriLock {

  private final URI uri;

  public NullUriLock(URI uri) {
    this.uri = uri;
  }

  /**
   * {@inheritDoc}
   */
  public URI getUri() {
    return uri;
  }
}
