/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.apache.commons.net.ftp.FTPCmd.MLST;
import static org.mule.extension.ftp.api.UriUtils.createUri;
import static org.mule.extension.ftp.api.UriUtils.trimLastFragment;
import static org.mule.extension.ftp.internal.FtpUtils.getReplyErrorMessage;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.net.ftp.FTPReply.isPositiveCompletion;

import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.extension.ftp.internal.operation.ListCommand;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.slf4j.Logger;

/**
 * A {@link FtpCommand} which implements the {@link ListCommand} contract
 *
 * @since 1.0
 */
public final class FtpListCommand extends FtpCommand implements ListCommand {

  private static final Logger LOGGER = getLogger(FtpListCommand.class);
  private static final int FTP_LIST_PAGE_SIZE = 25;
  private final FtpReadCommand ftpReadCommand;

  /**
   * {@inheritDoc}
   */
  public FtpListCommand(FtpFileSystem fileSystem, FTPClient client, FtpReadCommand ftpReadCommand) {
    super(fileSystem, client);
    this.ftpReadCommand = ftpReadCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config,
                                                      String directoryPath,
                                                      boolean recursive,
                                                      Predicate<FtpFileAttributes> matcher) {
    URI uri = resolvePath(normalizePath(directoryPath));

    if (!tryChangeWorkingDirectory(uri.getPath())) {

      FtpFileAttributes directoryAttributes = getExistingFile(directoryPath);

      if (!directoryAttributes.isDirectory()) {
        throw cannotListFileException(uri);
      }

      throw exception(format("Could not change working directory to '%s' while trying to list that directory", uri.getPath()));
    }

    List<Result<String, FtpFileAttributes>> accumulator = new LinkedList<>();

    try {
      doList(config, uri, accumulator, recursive, matcher);

      if (!isPositiveCompletion(client.getReplyCode())) {
        throw exception(format("Failed to list files on directory '%s'", uri.getPath()));
      }

      changeWorkingDirectory(uri.getPath());
    } catch (Exception e) {
      throw exception(format("Failed to list files on directory '%s'", uri.getPath()), e);
    }

    return accumulator;
  }

  private void doList(FileConnectorConfig config,
                      URI uri,
                      List<Result<String, FtpFileAttributes>> accumulator,
                      boolean recursive,
                      Predicate<FtpFileAttributes> matcher)
      throws IOException {
    LOGGER.debug("Listing directory {}", uri.getPath());

    Iterator<FTPFile[]> iterator = getFtpFileIterator();
    while (iterator.hasNext()) {
      FTPFile[] files = iterator.next();
      if (files == null || files.length == 0) {
        return;
      }

      for (FTPFile file : files) {
        if (file != null) {
          final URI fileUri = createUri(uri.getPath(), file.getName());
          FtpFileAttributes attributes = new FtpFileAttributes(fileUri, file);

          if (isVirtualDirectory(attributes.getName())) {
            continue;
          }

          if (attributes.isDirectory()) {
            if (matcher.test(attributes)) {
              accumulator.add(Result.<String, FtpFileAttributes>builder().output(null).attributes(attributes).build());
            }

            if (recursive) {
              URI recursionUri = createUri(uri.getPath(), normalizePath(attributes.getName()));
              if (!client.changeWorkingDirectory(attributes.getName())) {
                throw exception(format("Could not change working directory to '%s' while performing recursion on list operation",
                                       recursionUri.getPath()));
              }
              doList(config, recursionUri, accumulator, recursive, matcher);
              if (!client.changeToParentDirectory()) {
                throw exception(format("Could not return to parent working directory '%s' while performing recursion on list operation",
                                       trimLastFragment(recursionUri).getPath()));
              }
            }
          } else {
            if (matcher.test(attributes)) {
              accumulator.add(getFilePath(fileUri, attributes));
            }
          }
        }
      }
    }
  }

  private Result<String, FtpFileAttributes> getFilePath(URI fileUri, FtpFileAttributes attributes) {
    return Result.<String, FtpFileAttributes>builder().output(fileUri.getPath())
        .mediaType(MediaType.TEXT)
        .attributes(attributes).build();
  }

  private Iterator<FTPFile[]> getFtpFileIterator() throws IOException {
    // Check for MLST feature in accordance with rfc-3659, which states that
    // the presence of the MLST feature indicates that both MLST and MLSD are supported.
    if (fileSystem.isFeatureSupported(MLST.getCommand())) {
      try {
        FTPFile[] files = client.mlistDir();
        if (isPositiveCompletion(client.getReplyCode())) {
          return new SingleItemIterator(files);
        }
      } catch (MalformedServerReplyException e) {
        LOGGER
            .debug(format("Server answered the MLSD command with a MalformedServerReplyException. Falling back to the old LIST command. Exception message was: ",
                          e.getMessage()));
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER
            .debug(format("Server answered the MLSD command with a negative completion code. Falling back to the old LIST command. %s",
                          getReplyErrorMessage(client.getReplyCode(), client.getReplyString())));
      }
    }
    return new FtpListEngineIterator(client.initiateListParsing());
  }

  private class SingleItemIterator<T> implements Iterator<T> {

    private T item;
    private boolean hasNext = true;

    public SingleItemIterator(T item) {
      this.item = item;
    }

    public boolean hasNext() {
      return hasNext;
    }

    public T next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      hasNext = false;
      return item;
    }
  }

  private class FtpListEngineIterator implements Iterator<FTPFile[]> {

    FTPListParseEngine engine;

    public FtpListEngineIterator(FTPListParseEngine engine) {
      this.engine = engine;
    }

    public FTPFile[] next() {
      return engine.getNext(FTP_LIST_PAGE_SIZE);
    }

    public boolean hasNext() {
      return engine.hasNext();
    }
  }

}
