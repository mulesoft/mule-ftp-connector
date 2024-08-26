/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.config;

import org.mule.extension.ftp.internal.connection.FileSystem;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.RefName;

import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Generic contract for a config of a connector which operates over a {@link FileSystem}
 *
 * @since 1.0
 */
public abstract class FileConnectorConfig {

  @RefName
  private String configName;

  @DefaultEncoding
  private String muleEncoding;

  /**
   * @return the name that this config has on the mule registry
   */
  private String getConfigName() {
    return configName;
  }

  /**
   * @param timeBetweenSizeCheck      amount of time units.
   * @param timeBetweenSizeCheckUnit  {@link TimeUnit} that will be converted to milliseconds.
   * @return {@link Long} representing an amount of millisecond or null if {@param timeBetweenSizeCheck} is null
   */
  public java.util.Optional<Long> getTimeBetweenSizeCheckInMillis(Long timeBetweenSizeCheck, TimeUnit timeBetweenSizeCheckUnit) {
    return timeBetweenSizeCheck == null ? empty() : of(timeBetweenSizeCheckUnit.toMillis(timeBetweenSizeCheck));
  }
}
