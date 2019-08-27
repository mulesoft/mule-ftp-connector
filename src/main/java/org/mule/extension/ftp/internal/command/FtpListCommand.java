/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.command;

import static java.lang.String.format;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mule.extension.file.common.api.util.UriUtils.trimLastFragment;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ListCommand;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.connection.FtpFileSystem;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;

/**
 * A {@link FtpCommand} which implements the {@link ListCommand} contract
 *
 * @since 1.0
 */
public final class FtpListCommand extends FtpCommand implements ListCommand<FtpFileAttributes> {

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
  @Deprecated
  @Override
  public List<Result<InputStream, FtpFileAttributes>> list(FileConnectorConfig config,
                                                           String directoryPath,
                                                           boolean recursive,
                                                           Predicate<FtpFileAttributes> matcher) {
    return list(config, directoryPath, recursive, matcher, null);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public List<Result<InputStream, FtpFileAttributes>> list(FileConnectorConfig config,
                                                           String directoryPath,
                                                           boolean recursive,
                                                           Predicate<FtpFileAttributes> matcher,
                                                           Long timeBetweenSizeCheck) {
    URI uri = resolvePath(normalizePath(directoryPath));

    if (!tryChangeWorkingDirectory(uri.getPath())) {

      FileAttributes directoryAttributes = getExistingFile(directoryPath);

      if (!directoryAttributes.isDirectory()) {
        throw cannotListFileException(uri);
      }

      throw exception(format("Could not change working directory to '%s' while trying to list that directory", uri.getPath()));
    }

    List<Result<InputStream, FtpFileAttributes>> accumulator = new LinkedList<>();

    try {
      doList(config, uri, accumulator, recursive, matcher, timeBetweenSizeCheck);

      if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
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
                      List<Result<InputStream, FtpFileAttributes>> accumulator,
                      boolean recursive,
                      Predicate<FtpFileAttributes> matcher,
                      Long timeBetweenSizeCheck)
      throws IOException {
    LOGGER.debug("Listing directory {}", uri.getPath());

    Iterator<FTPFile[]> iterator = getIterator();
    while (iterator.hasNext()) {
      FTPFile[] files = iterator.next();
      if (files == null || files.length == 0) {
        return;
      }

      for (FTPFile file : files) {
        final URI fileUri = createUri(uri.getPath(), file.getName());
        FtpFileAttributes attributes = new FtpFileAttributes(fileUri, file);

        if (isVirtualDirectory(attributes.getName())) {
          continue;
        }

        if (attributes.isDirectory()) {
          if (matcher.test(attributes)) {
            accumulator.add(Result.<InputStream, FtpFileAttributes>builder().output(null).attributes(attributes).build());
          }

          if (recursive) {
            URI recursionUri = createUri(uri.getPath(), normalizePath(attributes.getName()));
            if (!client.changeWorkingDirectory(attributes.getName())) {
              throw exception(format("Could not change working directory to '%s' while performing recursion on list operation",
                                     recursionUri.getPath()));
            }
            doList(config, recursionUri, accumulator, recursive, matcher, timeBetweenSizeCheck);
            if (!client.changeToParentDirectory()) {
              throw exception(format("Could not return to parent working directory '%s' while performing recursion on list operation",
                                     trimLastFragment(recursionUri).getPath()));
            }
          }
        } else {
          if (matcher.test(attributes)) {
            accumulator.add(ftpReadCommand.read(config, attributes, false, timeBetweenSizeCheck));
          }
        }
      }
    }
  }

  private Iterator<FTPFile[]> getIterator() throws IOException {
    // Check if MLST command is supported
    try {
      return new SingleItemIterator(client.mlistDir());
    } catch (IOException ex) {
      LOGGER
          .debug("The FTP server does not seem to support the MLST command specified in the 'Extensions to FTP' RFC 3659. Server message was: \n"
              + ex.getMessage() + "\n Attempting again but with the LIST command.");
      return new EngineIterator(client.initiateListParsing());
    }
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

  private class EngineIterator implements Iterator<FTPFile[]> {

    FTPListParseEngine engine;

    public EngineIterator(FTPListParseEngine engine) {
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
