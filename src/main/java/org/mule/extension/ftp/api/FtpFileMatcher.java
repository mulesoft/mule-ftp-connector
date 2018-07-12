/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.extension.file.common.api.matcher.FileMatcher;

import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * A set of criterias used to filter files stored in a FTP server.
 *
 * @since 1.0
 */
@Alias("matcher")
@TypeDsl(allowTopLevelDefinition = true)
public class FtpFileMatcher extends FileMatcher<FtpFileMatcher, FtpFileAttributes> {

  /**
   * Files created before this date are rejected.
   * If no creation date is available, the File will be processed.
   */
  @Parameter
  @Optional
  private LocalDateTime timestampSince;

  /**
   * Files created after this date are rejected.
   * If no creation date is available, the File will be processed.
   */
  @Parameter
  @Optional
  private LocalDateTime timestampUntil;

  @Override
  protected Predicate<FtpFileAttributes> addConditions(Predicate<FtpFileAttributes> predicate) {
    if (timestampSince != null) {
      predicate = predicate.and(attributes -> attributes.getTimestamp() == null
        || FILE_TIME_SINCE.apply(timestampSince, attributes.getTimestamp()));
    }

    if (timestampUntil != null) {
      predicate = predicate.and(attributes -> attributes.getTimestamp() == null
        || FILE_TIME_UNTIL.apply(timestampUntil, attributes.getTimestamp()));
    }

    return predicate;
  }

  public FtpFileMatcher setTimestampSince(LocalDateTime timestampSince) {
    this.timestampSince = timestampSince;
    return this;
  }

  public FtpFileMatcher setTimestampUntil(LocalDateTime timestampUntil) {
    this.timestampUntil = timestampUntil;
    return this;
  }

  public LocalDateTime getTimestampSince() {
    return timestampSince;
  }

  public LocalDateTime getTimestampUntil() {
    return timestampUntil;
  }
}
