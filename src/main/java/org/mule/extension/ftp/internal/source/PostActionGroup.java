/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.source;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.model.display.PathModel.Location.EXTERNAL;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.DIRECTORY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.extension.ftp.api.FileAlreadyExistsException;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Path;

import java.nio.file.Paths;

/**
 * Groups post processing action parameters
 *
 * @since 1.1
 */
public class PostActionGroup {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostActionGroup.class);

  /**
   * Whether each file should be deleted after processing or not
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean autoDelete = false;

  /**
   * If provided, each processed file will be moved to a directory pointed by this path.
   */
  @Parameter
  @Optional
  @Path(type = DIRECTORY, location = EXTERNAL)
  private String moveToDirectory;

  /**
   * This parameter works in tandem with {@code moveToDirectory}. Use this parameter to enter the name under which the file should
   * be moved. Do not set this parameter if {@code moveToDirectory} hasn't been set as well.
   */
  @Parameter
  @Optional
  private String renameTo;

  /**
   * Whether any of the post actions ({@code autoDelete} and {@code moveToDirectory}) should also be applied in case the
   * file failed to be processed. If set to {@code false}, no failed files will be moved nor deleted.
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean applyPostActionWhenFailed = true;

  /**
   * Enables you to overwrite the target file when the destination file has the same name
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean overwrite = false;


  public PostActionGroup() {}

  public PostActionGroup(boolean autoDelete, String moveToDirectory, String renameTo, boolean applyPostActionWhenFailed) {
    this(autoDelete, moveToDirectory, renameTo, applyPostActionWhenFailed, false);
  }

  public PostActionGroup(boolean autoDelete, String moveToDirectory, String renameTo, boolean applyPostActionWhenFailed,
                         boolean overwrite) {
    this.autoDelete = autoDelete;
    this.moveToDirectory = moveToDirectory;
    this.renameTo = renameTo;
    this.applyPostActionWhenFailed = applyPostActionWhenFailed;
    this.overwrite = overwrite;
  }

  public boolean isAutoDelete() {
    return autoDelete;
  }

  public String getMoveToDirectory() {
    return moveToDirectory;
  }

  public String getRenameTo() {
    return renameTo;
  }

  public boolean isApplyPostActionWhenFailed() {
    return applyPostActionWhenFailed;
  }

  public boolean getOverwrite() {
    return overwrite;
  }

  public void validateSelf() throws IllegalArgumentException {
    if (isAutoDelete()) {
      if (getMoveToDirectory() != null) {
        throw new IllegalArgumentException(format("The autoDelete parameter was set to true, but the value '%s' was given to the "
            + "moveToDirectory parameter. These two are contradictory.", getMoveToDirectory()));
      } else if (getRenameTo() != null) {
        throw new IllegalArgumentException(format("The autoDelete parameter was set to true, but the value '%s' was given to the "
            + "renameTo parameter. These two are contradictory.", getRenameTo()));
      }
    }
  }

  public void apply(FtpFileSystem fileSystem, FtpFileAttributes ftpFileAttributes, FileConnectorConfig config) {
    if (LOGGER.isTraceEnabled()) {
      try {
        validateSelf();
      } catch (IllegalArgumentException e) {
        LOGGER.trace(e.getMessage());
      }
    }

    boolean movedOrRenamed = false;
    try {
      if (getMoveToDirectory() != null) {
        fileSystem.move(config, ftpFileAttributes.getPath(), getMoveToDirectory(), getOverwrite(), true,
                        getRenameTo());
        movedOrRenamed = true;
      } else if (getRenameTo() != null) {
        fileSystem.rename(ftpFileAttributes.getPath(), getRenameTo(), getOverwrite());
        movedOrRenamed = true;
      }
    } catch (FileAlreadyExistsException e) {
      if (!isAutoDelete()) {
        if (getMoveToDirectory() == null) {
          LOGGER.warn(format("A file with the same name was found when trying to rename '%s' to '%s'" +
              ". The file '%s' was not renamed and it remains on the poll directory.",
                             ftpFileAttributes.getName(), getRenameTo(), ftpFileAttributes.getPath()));
        } else {
          String moveToFileName = getRenameTo() == null ? ftpFileAttributes.getName() : getRenameTo();
          String moveToPath = Paths.get(getMoveToDirectory()).resolve(moveToFileName).toString();
          LOGGER.warn(format("A file with the same name was found when trying to move '%s' to '%s'" +
              ". The file '%s' was not sent to the moveTo directory and it remains on the poll directory.",
                             ftpFileAttributes.getPath(), moveToPath, ftpFileAttributes.getPath()));
        }
        throw e;
      }
    } finally {
      if (isAutoDelete() && !movedOrRenamed) {
        fileSystem.delete(ftpFileAttributes.getPath());
      }
    }
  }
}
