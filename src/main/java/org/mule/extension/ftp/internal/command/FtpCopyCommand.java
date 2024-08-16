/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.ftp.internal.util.UriUtils.createUri;

import org.mule.extension.ftp.api.FileWriteMode;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.FtpCopyDelegate;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.operation.CopyCommand;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link FtpCommand} which implements the {@link CopyCommand} contract
 *
 * @since 1.0
 */
public final class FtpCopyCommand extends FtpCommand implements CopyCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpCopyCommand.class);


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

  /**
   * Concrete implementation of {@link FtpCopyDelegate} for copying operations which require to FTP connections, one for reading the
   * source file and another for writing into the target path
   *
   * @since 1.0
   */
  private class RegularFtpCopyDelegate implements FtpCopyDelegate {

    private final FtpCommand command;
    private final FtpFileSystem fileSystem;
    private final Logger LOGGER = LoggerFactory.getLogger(RegularFtpCopyDelegate.class);

    public RegularFtpCopyDelegate(FtpCommand command, FtpFileSystem fileSystem) {
      this.command = command;
      this.fileSystem = fileSystem;
    }

    /**
     * Performs a recursive copy of a directory
     * @param config the config which is parameterizing this operation
     * @param sourceUri the path to the directory to be copied
     * @param targetUri the target path
     * @param overwrite whether to overwrite the target files if they already exists
     * @param writerConnection the {@link FtpFileSystem} which connects to the target endpoint
     */
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
        if (file != null && isVirtualDirectory(file.getName())) {
          continue;
        }

        FtpFileAttributes ftpFileAttributes = new FtpFileAttributes(createUri(sourceUri.getPath(), file.getName()), file);
        String filePath = ftpFileAttributes.getPath();

        URI targetFileUri = createUri(targetUri.getPath(), ftpFileAttributes.getFileName());
        if (ftpFileAttributes.isDirectory()) {
          LOGGER.trace("Copy directory {} to {}", filePath, targetUri);

          copyDirectory(config, createUri(filePath), targetFileUri, overwrite, writerConnection);
        } else {
          LOGGER.trace("Copy file {} to {}", filePath, targetUri);

          copyFile(config, ftpFileAttributes, targetFileUri, overwrite, writerConnection);
        }
      }
    }

    /**
     * Copies one individual file
     *  @param config the config which is parameterizing this operation
     * @param source the {@link FtpFileAttributes} for the file to be copied
     * @param target the target path
     * @param overwrite whether to overwrite the target files if they already exists
     * @param writerConnection the {@link FtpFileSystem} which connects to the target endpoint
     */
    protected void copyFile(FileConnectorConfig config, FtpFileAttributes source, URI target, boolean overwrite,
                            FtpFileSystem writerConnection) {
      FtpFileAttributes targetFile = command.getFile(target.getPath());
      if (targetFile != null) {
        if (overwrite) {
          fileSystem.delete(targetFile.getPath());
        } else {
          throw command.alreadyExistsException(target);
        }
      }

      try (InputStream inputStream = fileSystem.retrieveFileContent(source)) {
        if (inputStream == null) {
          throw command
              .exception(format("Could not read file '%s' while trying to copy it to remote path '%s'", source.getPath(),
                                target.getPath()));
        }

        writeCopy(config, target.getPath(), inputStream, overwrite, writerConnection);
      } catch (Exception e) {
        throw command
            .exception(format("Found exception while trying to copy file '%s' to remote path '%s'", source.getPath(),
                              target.getPath()),
                       e);
      }
      fileSystem.awaitCommandCompletion();
    }

    private void writeCopy(FileConnectorConfig config, String targetPath, InputStream inputStream, boolean overwrite,
                           FtpFileSystem writerConnection)
        throws IOException {
      final FileWriteMode mode = overwrite ? FileWriteMode.OVERWRITE : FileWriteMode.CREATE_NEW;
      writerConnection.write(targetPath, inputStream, mode, false, true);
    }

    private ConnectionHandler<FtpFileSystem> getWriterConnection(FileConnectorConfig config) throws ConnectionException {
      return ((FtpConnector) config).getConnectionManager().getConnection(config);
    }

    /**
     * Performs a recursive copy
     *  @param config the config which is parameterizing this operation
     * @param source the {@link FtpFileAttributes} for the file to be copied
     * @param targetUri the {@link URI} to the target destination
     * @param overwrite whether to overwrite existing target paths
     */
    @Override
    public void doCopy(FileConnectorConfig config, FtpFileAttributes source, URI targetUri, boolean overwrite) {
      ConnectionHandler<FtpFileSystem> writerConnectionHandler;
      final FtpFileSystem writerConnection;
      String path = source.getPath();
      try {
        writerConnectionHandler = getWriterConnection(config);
        writerConnection = writerConnectionHandler.getConnection();
      } catch (ConnectionException e) {
        throw command
            .exception(format("FTP Copy operations require the use of two FTP connections. An exception was found trying to obtain second connection to"
                + "copy the path '%s' to '%s'", path, targetUri.getPath()), e);
      }
      try {
        if (source.isDirectory()) {
          copyDirectory(config, createUri(path), targetUri, overwrite, writerConnection);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Copied directory {} to {}", path, targetUri);
          }
        } else {
          copyFile(config, source, targetUri, overwrite, writerConnection);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Copied file {} to {}", path, targetUri.getPath());
          }
        }
      } catch (ModuleException e) {
        throw e;
      } catch (Exception e) {
        throw command.exception(format("Found exception copying file '%s' to '%s'", source, targetUri.getPath()), e);
      } finally {
        writerConnectionHandler.release();
      }
    }
  }
}
