/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.CopyCommand;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.AbstractFtpCopyDelegate;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * A {@link FtpCommand} which implements the {@link CopyCommand} contract
 *
 * @since 1.0
 */
public final class FtpCopyCommand extends FtpCommand implements CopyCommand {

  /**
   * {@inheritDoc}
   */
  public FtpCopyCommand(FtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void copy(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                   boolean createParentDirectories, String renameTo) {
    copy(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo,
         new RegularFtpCopyDelegate(this, fileSystem));
  }

  private class RegularFtpCopyDelegate extends AbstractFtpCopyDelegate {

    public RegularFtpCopyDelegate(FtpCommand command, FtpFileSystem fileSystem) {
      super(command, fileSystem);
    }

    @Override
    protected void copyDirectory(FileConnectorConfig config, URI sourceUri, URI targetUri, boolean overwrite,
                                 FtpFileSystem writerConnection) {
      changeWorkingDirectory(sourceUri.getPath());
      FTPFile[] files;
      try {
        files = client.listFiles();
      } catch (IOException e) {
        throw exception(format("Could not list contents of directory '%s' while trying to copy it to %s", sourceUri.getPath(),
                               targetUri.getPath()),
                        e);
      }

      for (FTPFile file : files) {
        if (isVirtualDirectory(file.getName())) {
          continue;
        }

        FileAttributes fileAttributes = new FtpFileAttributes(createUri(sourceUri.getPath(), file.getName()), file);

        URI targetFileUri = createUri(targetUri.getPath(), fileAttributes.getName());
        if (fileAttributes.isDirectory()) {
          copyDirectory(config, createUri(fileAttributes.getPath()), targetFileUri, overwrite, writerConnection);
        } else {
          copyFile(config, fileAttributes, targetFileUri, overwrite, writerConnection);
        }
      }
    }

    @Override
    protected void copyFile(FileConnectorConfig config, FileAttributes source, URI target, boolean overwrite,
                            FtpFileSystem writerConnection) {
      super.copyFile(config, source, target, overwrite, writerConnection);
      fileSystem.awaitCommandCompletion();
    }
  }
}
