/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.extension.file.common.api.matcher.FileMatcher;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * A set of criteria used to filter files stored in a FTP server. The file's properties are to be represented on
 * an instance of {@link FtpFileAttributes}.
 *
 * @since 1.0
 */
@Alias("matcher")
@TypeDsl(allowTopLevelDefinition = true)
public class FtpFileMatcher extends FileMatcher<FtpFileMatcher, FtpFileAttributes> {

  private static final Logger LOGGER = getLogger(FtpFileMatcher.class);
  private AtomicBoolean alreadyLoggedWarning = new AtomicBoolean();


  /**
   * Files created before this date are rejected.
   * If no creation date is available, the File will be processed.
   */
  @Parameter
  @Summary("Files created before this date are rejected. If no creation date is available, the File will be processed.")
  @Example("2015-06-03T13:21:58+00:00")
  @Optional
  private LocalDateTime timestampSince;

  /**
   * Files created after this date are rejected.
   * If no creation date is available, the File will be processed.
   */
  @Parameter
  @Summary("Files created after this date are rejected. If no creation date is available, the File will be processed.")
  @Example("2015-06-03T13:21:58+00:00")
  @Optional
  private LocalDateTime timestampUntil;

  /**
   * Minimum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with {@link #timeUnit}.
   */
  @Parameter
  @Summary("Minimum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with timeUnit.")
  @Example("10000")
  @Optional
  private Long notUpdatedInTheLast;

  /**
   * Maximum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with {@link #timeUnit}.
   */
  @Parameter
  @Summary("Maximum time that should have passed since a file was updated to not be rejected. This attribute works in tandem with timeUnit.")
  @Example("10000")
  @Optional
  private Long updatedInTheLast;

  /**
   * A {@link TimeUnit} which qualifies the {@link #updatedInTheLast} and the {@link #notUpdatedInTheLast} attributes.
   * <p>
   * Defaults to {@code SECONDS}
   */
  @Parameter
  @Summary("Time unit to be used to interpret the parameters 'notUpdatedInTheLast' and 'updatedInTheLast'")
  @Optional(defaultValue = "SECONDS")
  private TimeUnit timeUnit;

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

    // We want to make sure that the same time is used when comparing multiple files consecutively.
    LocalDateTime now = now();

    if (notUpdatedInTheLast != null) {
      predicate = predicate.and(attributes -> {
        checkTimestampPrecision(attributes);
        return attributes.getTimestamp() == null
            || FILE_TIME_UNTIL.apply(minusTime(now, notUpdatedInTheLast, timeUnit), attributes.getTimestamp());
      });
    }

    if (updatedInTheLast != null) {
      predicate = predicate.and(attributes -> {
        checkTimestampPrecision(attributes);
        return attributes.getTimestamp() == null
            || FILE_TIME_SINCE.apply(minusTime(now, updatedInTheLast, timeUnit), attributes.getTimestamp());
      });
    }

    return predicate;
  }

  private void checkTimestampPrecision(FtpFileAttributes attributes) {
    if (alreadyLoggedWarning.compareAndSet(false, true) && isSecondsOrLower(timeUnit)
        && attributes.getTimestamp().getSecond() == 0 && attributes.getTimestamp().getNano() == 0) {
      LOGGER
          .debug(format("The timestamp precision was set to %s, but it seems like the server does not support such precision.",
                        timeUnit));
    }
  }

  private boolean isSecondsOrLower(TimeUnit timeUnit) {
    return timeUnit == TimeUnit.SECONDS || timeUnit == TimeUnit.MILLISECONDS || timeUnit == TimeUnit.MICROSECONDS
        || timeUnit == TimeUnit.NANOSECONDS;
  }

  private LocalDateTime minusTime(LocalDateTime localDateTime, Long time, TimeUnit timeUnit) {
    return localDateTime.minus(getTimeInMillis(time, timeUnit), ChronoUnit.MILLIS);
  }

  private long getTimeInMillis(Long time, TimeUnit timeUnit) {
    return timeUnit.toMillis(time);
  }

  public FtpFileMatcher setTimestampSince(LocalDateTime timestampSince) {
    this.timestampSince = timestampSince;
    return this;
  }

  public FtpFileMatcher setTimestampUntil(LocalDateTime timestampUntil) {
    this.timestampUntil = timestampUntil;
    return this;
  }

  public FtpFileMatcher setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
    return this;
  }

  public FtpFileMatcher setUpdatedInTheLast(Long updatedInTheLast) {
    this.updatedInTheLast = updatedInTheLast;
    return this;
  }

  public FtpFileMatcher setNotUpdatedInTheLast(Long notUpdatedInTheLast) {
    this.notUpdatedInTheLast = notUpdatedInTheLast;
    return this;
  }

  public LocalDateTime getTimestampSince() {
    return timestampSince;
  }

  public LocalDateTime getTimestampUntil() {
    return timestampUntil;
  }

  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  public Long getUpdatedInTheLast() {
    return updatedInTheLast;
  }

  public Long getNotUpdatedInTheLast() {
    return notUpdatedInTheLast;
  }
}
