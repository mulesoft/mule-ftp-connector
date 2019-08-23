/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.source;

import static java.lang.String.format;
import static org.mule.extension.file.common.api.FileDisplayConstants.MATCHER;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.runtime.source.PollContext.PollItemStatus.SOURCE_STOPPING;

import org.mule.extension.file.common.api.matcher.NullFilePayloadPredicate;
import org.mule.extension.ftp.api.FtpFileMatcher;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollContext.PollItemStatus;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls a directory looking for files that have been created on it. One message will be generated for each file that is found.
 * <p>
 * The key part of this functionality is how to determine that a file is actually new. There're three strategies for that:
 * <ul>
 * <li>Set the <i>autoDelete</i> parameter to <i>true</i>: This will delete each processed file after it has been processed,
 * causing all files obtained in the next poll to be necessarily new</li>
 * <li>Set <i>moveToDirectory</i> parameter: This will move each processed file to a different directory after it has been
 * processed, achieving the same effect as <i>autoDelete</i> but without loosing the file</li>
 * <li></li>
 * <li>Use the <i>watermarkEnabled</i> parameter to only pick files that have been created or updated after the last poll was
 * executed.</li>
 * </ul>
 * <p>
 * A matcher can also be used for additional filtering of files.
 *
 * @since 1.1
 */
@MediaType(value = ANY, strict = false)
@DisplayName("On New or Updated File")
@Summary("Triggers when a new file is created in a directory")
@Alias("listener")
// TODO: MULE-13940 - add mimeType here too
public class FtpDirectoryListener extends PollingSource<InputStream, FtpFileAttributes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpDirectoryListener.class);
  private static final String ATTRIBUTES_CONTEXT_VAR = "attributes";
  private static final String POST_PROCESSING_GROUP_NAME = "Post processing action";

  @Config
  private FtpConnector config;

  @Connection
  private ConnectionProvider<FtpFileSystem> fileSystemProvider;

  /**
   * The directory on which polled files are contained
   */
  @Parameter
  @Optional
  private String directory;

  /**
   * Whether or not to also files contained in sub directories.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Summary("Whether or not to also catch files created on sub directories")
  private boolean recursive = true;

  /**
   * A matcher used to filter events on files which do not meet the matcher's criteria
   */
  @Parameter
  @Optional
  @Alias("matcher")
  @DisplayName(MATCHER)
  private FtpFileMatcher predicateBuilder;

  /**
   * Controls whether or not to do watermarking, and if so, if the watermark should consider the file's modification or creation
   * timestamps
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean watermarkEnabled = false;

  /**
   * Wait time in milliseconds between size checks to determine if a file is ready to be read. This allows a file write to
   * complete before processing. You can disable this feature by omitting a value. When enabled, Mule performs two size checks
   * waiting the specified time between calls. If both checks return the same value, the file is ready to be read.
   */
  @Parameter
  @ConfigOverride
  @Summary("Wait time in milliseconds between size checks to determine if a file is ready to be read.")
  private Long timeBetweenSizeCheck;

  /**
   * A {@link TimeUnit} which qualifies the {@link #timeBetweenSizeCheck} attribute.
   */
  @Parameter
  @ConfigOverride
  @Summary("Time unit to be used in the wait time between size checks")
  private TimeUnit timeBetweenSizeCheckUnit;

  private Path directoryPath;
  private Predicate<FtpFileAttributes> matcher;

  @Override
  protected void doStart() {
    refreshMatcher();
    directoryPath = resolveRootPath();
  }

  @Override
  protected void doStop() {

  }

  @OnSuccess
  public void onSuccess(@ParameterGroup(name = POST_PROCESSING_GROUP_NAME) PostActionGroup postAction,
                        SourceCallbackContext ctx) {
    postAction(postAction, ctx);
  }

  @OnError
  public void onError(@ParameterGroup(name = POST_PROCESSING_GROUP_NAME) PostActionGroup postAction,
                      SourceCallbackContext ctx) {
    if (postAction.isApplyPostActionWhenFailed()) {
      postAction(postAction, ctx);
    }
  }

  @OnTerminate
  public void onTerminate(SourceCallbackContext ctx) {
    FtpFileSystem fileSystem = ctx.getConnection();
    if (fileSystem != null) {
      fileSystemProvider.disconnect(fileSystem);
    }
  }

  @Override
  public void poll(PollContext<InputStream, FtpFileAttributes> pollContext) {
    refreshMatcher();
    if (pollContext.isSourceStopping()) {
      return;
    }

    FtpFileSystem fileSystem;
    try {
      fileSystem = openConnection();
    } catch (Exception e) {
      if (e instanceof ConnectionException) {
        pollContext.onConnectionException((ConnectionException) e);
      }
      LOGGER.error(format("Could not obtain connection while trying to poll directory '%s'. %s", directoryPath.toString(),
                          e.getMessage()),
                   e);

      return;
    }

    try {
      List<Result<InputStream, FtpFileAttributes>> files =
          fileSystem
              .list(config, directoryPath.toString(), recursive, matcher,
                    config.getTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit).orElse(null));

      if (files.isEmpty()) {
        return;
      }
      for (Result<InputStream, FtpFileAttributes> file : files) {

        FtpFileAttributes attributes = file.getAttributes().orElse(null);

        if (attributes == null || attributes.isDirectory()) {
          continue;
        }

        if (!matcher.test(attributes)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Skipping file '{}' because the matcher rejected it", attributes.getPath());
          }
          return;
        }

        if (!processFile(file, pollContext)) {
          break;
        }
      }
    } catch (Exception e) {
      LOGGER.error(format("Found exception trying to poll directory '%s'. Will try again on the next poll. ",
                          directoryPath.toString(), e.getMessage()),
                   e);
    } finally {
      if (fileSystem != null) {
        fileSystemProvider.disconnect(fileSystem);
      }
    }
  }

  private void refreshMatcher() {
    matcher = predicateBuilder != null ? predicateBuilder.build() : new NullFilePayloadPredicate<>();
  }

  @Override
  public void onRejectedItem(Result<InputStream, FtpFileAttributes> result, SourceCallbackContext callbackContext) {
    closeQuietly(result.getOutput());
  }

  private FtpFileSystem openConnection() throws Exception {

    FtpFileSystem fileSystem = fileSystemProvider.connect();
    try {
      fileSystem.changeToBaseDir();
    } catch (Exception e) {
      fileSystemProvider.disconnect(fileSystem);
      throw e;
    }
    return fileSystem;
  }

  private boolean processFile(Result<InputStream, FtpFileAttributes> file,
                              PollContext<InputStream, FtpFileAttributes> pollContext) {
    FtpFileAttributes attributes = file.getAttributes().get();
    String fullPath = attributes.getPath();

    PollItemStatus status = pollContext.accept(item -> {
      SourceCallbackContext ctx = item.getSourceCallbackContext();
      Result<InputStream, FtpFileAttributes> result = null;
      try {
        ctx.addVariable(ATTRIBUTES_CONTEXT_VAR, attributes);
        item.setResult(file);
        item.setId(attributes.getPath());
        if (watermarkEnabled) {
          if (attributes.getTimestamp() != null) {
            item.setWatermark(attributes.getTimestamp());
          } else {
            LOGGER.warn(format("Use of watermark for files processing is enabled, but file [%s] does not have the"
                + " corresponding modification timestamp. Watermark ignored for this file.",
                               fullPath));
          }
        }
      } catch (Throwable t) {
        LOGGER.error(format("Found file '%s' but found exception trying to dispatch it for processing. %s",
                            fullPath, t.getMessage()),
                     t);
        if (result != null) {
          onRejectedItem(result, ctx);
        }
      }
    });

    return status != SOURCE_STOPPING;
  }

  private void postAction(PostActionGroup postAction, SourceCallbackContext ctx) {
    ctx.<FtpFileAttributes>getVariable(ATTRIBUTES_CONTEXT_VAR).ifPresent(attrs -> {
      FtpFileSystem fileSystem = null;
      try {
        fileSystem = fileSystemProvider.connect();
        fileSystem.changeToBaseDir();
        postAction.apply(fileSystem, attrs, config);
      } catch (ConnectionException e) {
        LOGGER
            .error("An error occurred while retrieving a connection to apply the post processing action to the file {} , it was neither moved nor deleted.",
                   attrs.getPath());
      } finally {
        if (fileSystem != null) {
          fileSystemProvider.disconnect(fileSystem);
        }
      }
    });
  }

  private Path resolveRootPath() {
    FtpFileSystem fileSystem = null;
    try {
      fileSystem = fileSystemProvider.connect();
      return new OnNewFileCommand(fileSystem).resolveRootPath(directory);
    } catch (Exception e) {
      throw new MuleRuntimeException(I18nMessageFactory.createStaticMessage(
                                                                            format("Could not resolve path to directory '%s'. %s",
                                                                                   directory, e.getMessage())),
                                     e);
    } finally {
      if (fileSystem != null) {
        fileSystemProvider.disconnect(fileSystem);
      }
    }
  }
}
