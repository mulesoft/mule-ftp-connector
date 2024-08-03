/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;

import org.mule.extension.ftp.internal.connection.AbstractFileSystem;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.operation.CopyCommand;
import org.mule.extension.ftp.internal.operation.CreateDirectoryCommand;
import org.mule.extension.ftp.internal.operation.DeleteCommand;
import org.mule.extension.ftp.internal.operation.ListCommand;
import org.mule.extension.ftp.internal.operation.MoveCommand;
import org.mule.extension.ftp.internal.operation.ReadCommand;
import org.mule.extension.ftp.internal.operation.RenameCommand;
import org.mule.extension.ftp.internal.operation.WriteCommand;
import org.mule.extension.ftp.internal.exception.FileAlreadyExistsException;
import org.mule.extension.ftp.internal.lock.PathLock;
import org.mule.extension.ftp.internal.source.AbstractPostActionGroup;
import org.mule.extension.ftp.internal.source.PostActionGroup;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.Test;

@SmallTest
@Feature(FTP_EXTENSION)
public class PostActionGroupTestCase extends AbstractMuleTestCase {

  public static final String MOVE = "move";

  @Test
  @Description("tests all the valid states of post action parameters")
  public void validAction() {
    new PostActionGroup(true, null, null, true).validateSelf();
    new PostActionGroup(true, null, null, false).validateSelf();
    new PostActionGroup(false, "someDir", null, false).validateSelf();
    new PostActionGroup(false, "someDir", "thisone.txt", false).validateSelf();
    new PostActionGroup(false, null, "thisone.txt", false).validateSelf();
  }

  @Test(expected = IllegalArgumentException.class)
  @Description("verifies that autoDelete and moveToDirectory cannot be set at the same time")
  public void deleteAndMove() {
    new PostActionGroup(true, "someDir", null, true).validateSelf();
  }

  @Test(expected = IllegalArgumentException.class)
  @Description("verifies that autoDelete and renameTo cannot be set at the same time")
  public void deleteAndRename() {
    new PostActionGroup(true, null, "thisone.txt", true).validateSelf();
  }


  @Test(expected = FileAlreadyExistsException.class)
  @Description("verifies that move file with overwrite flag set to false and exceptiopn will be thrown")
  public void testMoveWithoutOverwriteWhenFileExists() {
    FtpFileAttributes ftpFileAttributes = mock(FtpFileAttributes.class);
    when(ftpFileAttributes.getFileName()).thenReturn("");
    FileConnectorConfig fileConnectorConfig = mock(FileConnectorConfig.class);

    ConcreteFileSystem fileSystem = new ConcreteFileSystem("");

    new PostActionGroupChild("someDir", null, false, false).apply(fileSystem, ftpFileAttributes, fileConnectorConfig);
  }

  @Test
  @Description("verifies that move file with overwrite flag set to true and no exception")
  public void testMoveWithOverwriteWhenFileExists() {
    FtpFileAttributes ftpFileAttributes = mock(FtpFileAttributes.class);
    when(ftpFileAttributes.getFileName()).thenReturn("");
    FileConnectorConfig fileConnectorConfig = mock(FileConnectorConfig.class);

    ConcreteFileSystem fileSystem = new ConcreteFileSystem("");
    fileSystem.setCanMove(true);
    new PostActionGroupChild("someDir", null, false, true).apply(fileSystem, ftpFileAttributes, fileConnectorConfig);
  }

  private class PostActionGroupChild extends AbstractPostActionGroup {

    private String moveToDirectory;
    private String renameTo;
    private boolean isAutoDelete;
    private boolean overwrite;

    public PostActionGroupChild(String moveToDirectory, String renameTo, boolean isAutoDelete, boolean overwrite) {
      this.moveToDirectory = moveToDirectory;
      this.renameTo = renameTo;
      this.isAutoDelete = isAutoDelete;
      this.overwrite = overwrite;
    }

    @Override
    public boolean isAutoDelete() {
      return isAutoDelete;
    }

    @Override
    public String getMoveToDirectory() {
      return moveToDirectory;
    }

    @Override
    public String getRenameTo() {
      return renameTo;
    }

    @Override
    public boolean isApplyPostActionWhenFailed() {
      return false;
    }

    public boolean getOverwrite() {
      return overwrite;
    }

  }
  private class Success implements Command {

    private Queue<String> actions;

    public Success(Queue<String> actions) {
      this.actions = actions;
    }

    @Override
    public void execute(String action) {
      actions.add(action);
    }
  }

  private class ConcreteFileSystem extends AbstractFileSystem {

    private Queue<String> actions;
    private boolean canRename = false;
    private boolean canMove = false;
    private Command success;

    public ConcreteFileSystem(String basePath) {
      super(basePath);
      this.actions = new LinkedList<>();
      this.success = new Success(actions);
    }

    public void clearActions() {
      this.actions.clear();
    }

    public String getActionExecuted() {
      return this.actions.element();
    }

    public void setCanRename(boolean canRename) {
      this.canRename = canRename;
    }

    public void setCanMove(boolean canMove) {
      this.canMove = canMove;
    }

    @Override
    protected ListCommand getListCommand() {
      return null;
    }

    @Override
    protected ReadCommand getReadCommand() {
      return null;
    }

    @Override
    protected WriteCommand getWriteCommand() {
      return null;
    }

    @Override
    protected CopyCommand getCopyCommand() {
      return null;
    }

    @Override
    protected MoveCommand getMoveCommand() {
      return new ConcreteCommand(canMove, success);
    }

    @Override
    protected DeleteCommand getDeleteCommand() {
      return new ConcreteCommand(true, success);
    }

    @Override
    protected RenameCommand getRenameCommand() {
      return new ConcreteCommand(canRename, success);
    }

    @Override
    protected CreateDirectoryCommand getCreateDirectoryCommand() {
      return null;
    }

    @Override
    protected PathLock createLock(Path path) {
      return null;
    }

    @Override
    public void changeToBaseDir() {

    }

    private class ConcreteCommand implements RenameCommand, MoveCommand, DeleteCommand {

      private boolean available;
      private Command successCallback;

      public ConcreteCommand(boolean available, Command successCallback) {
        this.available = available;
        this.successCallback = successCallback;
      }

      @Override
      public void rename(String filePath, String newName, boolean overwrite) {}

      @Override
      public void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                       boolean createParentDirectories, String renameTo) {
        if (!this.available)
          throw new FileAlreadyExistsException(MOVE);
        this.successCallback.execute(MOVE);
      }

      @Override
      public void delete(String filePath) {

      }
    }
  }

  private interface Command {

    void execute(String action);
  }
}
