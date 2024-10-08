/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.stream;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Decorate a {@link Supplier} so that the first invokation to the {@link #get()} method is actually forwarded to it, but
 * subsequent ones return the same cached value. This happens on a thread-safe (yet low contention) manner
 *
 * @since 1.0
 */
public final class LazyStreamSupplier implements Supplier<InputStream> {

  private volatile InputStream stream;
  private Supplier<InputStream> delegate;
  private boolean supplied = false;

  public LazyStreamSupplier(Supplier<InputStream> streamFactory) {
    delegate = () -> {
      synchronized (this) {
        if (!supplied) {
          supplied = true;
          stream = streamFactory.get();
          delegate = () -> stream;
        }
        return stream;
      }
    };
  }

  /**
   * @return a {@link InputStream}
   */
  @Override
  public InputStream get() {
    return delegate.get();
  }

  /**
   * @return whether {@link #get()} has ever been invoked on {@code this} instance
   */
  public boolean isSupplied() {
    return supplied;
  }
}
