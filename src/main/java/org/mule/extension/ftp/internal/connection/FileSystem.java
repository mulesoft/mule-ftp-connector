/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.connection;

import org.mule.extension.ftp.internal.lock.PathLock;
import org.mule.extension.ftp.api.FileWriteMode;
import org.mule.extension.ftp.internal.lock.UriLock;
import org.mule.extension.ftp.internal.operation.CopyCommand;
import org.mule.extension.ftp.internal.operation.CreateDirectoryCommand;
import org.mule.extension.ftp.internal.operation.DeleteCommand;
import org.mule.extension.ftp.internal.operation.ListCommand;
import org.mule.extension.ftp.internal.operation.MoveCommand;
import org.mule.extension.ftp.internal.operation.ReadCommand;
import org.mule.extension.ftp.internal.operation.RenameCommand;
import org.mule.extension.ftp.internal.operation.WriteCommand;
import org.mule.extension.ftp.internal.subset.SubsetList;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.internal.config.FileConnectorConfig;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.runtime.extension.api.runtime.operation.Result;

import javax.activation.MimetypesFileTypeMap;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

/**
 * Represents an abstract file system and the operations which can be performed on it.
 * <p>
 * This interface acts as a facade which allows performing common files operations regardless of those files being in a local
 * disk, an FTP server, a cloud storage service, etc.
 *
 * @since 1.0
 */
public interface FileSystem {

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} argument. If {@code recursive} is enabled, then all the files in that directory will be
   * listed immediately after their parent directory.
   * <p>
   *
   * @param config        the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive     whether to include the contents of sub-directories
   * @param matcher       a {@link Predicate} of {@link FtpFileAttributes} used to filter the output list
   * @return a {@link List} of {@link Result} objects, each one containing each file's content in the payload and metadata in the attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config,
                                               String directoryPath,
                                               boolean recursive,
                                               Predicate<FtpFileAttributes> matcher);

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} argument. If {@code recursive} is enabled, then all the files in that directory will be listed immediately
   * after their parent directory.
   * <p>
   *
   * @param config        the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive     whether to include the contents of sub-directories
   * @param matcher       a {@link Predicate} of {@link FtpFileAttributes} used to filter the output list
   * @param subsetList    parameter group that lets you obtain a subset of the results
   * @return a {@link List} of {@link Result} objects, each one containing each file's content in the payload and metadata in the
   * attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  List<Result<String, FtpFileAttributes>> list(FileConnectorConfig config,
                                               String directoryPath,
                                               boolean recursive,
                                               Predicate<FtpFileAttributes> matcher,
                                               SubsetList subsetList);

  /**
   * Obtains the content and metadata of a file at a given path.
   * <p>
   * Locking can be actually enabled through the {@code lock} argument, however, the extent of such lock will depend on the
   * implementation. What is guaranteed by passing {@code true} on the {@code lock} argument is that {@code this} instance will
   * not attempt to modify this file until the {@link InputStream} returned by {@link Result#getOutput()} this method returns is
   * closed or fully consumed. Some implementation might actually perform a file system level locking which goes beyond the extend
   * of {@code this} instance or even mule. For some other file systems that might be simply not possible and no extra assumptions
   * are to be taken.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. a {@link MimetypesFileTypeMap} will
   * be used to make an educated guess on the file's mime type
   *
   * @param config                the config that is parameterizing this operation
   * @param filePath              the path of the file you want to read
   * @param lock                  whether or not to lock the file
   * @param timeBetweenSizeCheck  wait time between size checks to determine if a file is ready to be read in milliseconds.
   * @return An {@link Result} with an {@link InputStream} with the file's content as payload and a {@link FtpFileAttributes} object
   *         as {@link Message#getAttributes()}
   * @throws IllegalArgumentException if the file at the given path doesn't exist
   */
  Result<InputStream, FtpFileAttributes> read(FileConnectorConfig config, String filePath, boolean lock,
                                              Long timeBetweenSizeCheck);

  /**
   * Writes the {@code content} into the file pointed by {@code filePath}.
   * <p>
   * The {@code content} can be of any of the given types:
   * <ul>
   * <li>{@link String}</li>
   * <li>{@code String[]}</li>
   * <li>{@code byte}</li>
   * <li>{@code byte[]}</li>
   * <li>{@link OutputHandler}</li>
   * <li>{@link Iterable}</li>
   * <li>{@link Iterator}</li>
   * </ul>
   * <p>
   * {@code null} contents are not allowed and will result in an {@link IllegalArgumentException}.
   * <p>
   * If the directory on which the file is attempting to be written doesn't exist, then the operation will either throw
   * {@link IllegalArgumentException} or create such folder depending on the value of the {@code createParentDirectory}.
   * <p>
   * If the file itself already exists, then the behavior depends on the supplied {@code mode}.
   * <p>
   * This method also supports locking support depending on the value of the {@code lock} argument, but following the same rules
   * and considerations as described in the {@link #read(FileConnectorConfig, String, boolean, Long)} method
   *
   * @param filePath the path of the file to be written
   * @param content the content to be written into the file
   * @param mode a {@link FileWriteMode}
   * @param lock whether or not to lock the file
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * 
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectories);



  /**
  * Copies the file at the {@code sourcePath} into the {@code targetPath}.
  * <p>
  * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
  * value of the {@code createParentDirectory} argument. If such argument is {@false}, then an {@link IllegalArgumentException}
  * will be thrown.
  * <p>
  * It is also possible to use the {@code targetPath} to specify that the copied file should also be renamed. For example, if
  * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
  * file will indeed be copied to the <i>a/c/</i> directory but renamed as <i>test.json</i>
  * <p>
  * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
  * {@link IllegalArgumentException} will be thrown
  * <p>
  * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be copied
  * recursively
  *
  * @param config                  the config that is parameterizing this operation
  * @param sourcePath              the path to the file to be copied
  * @param targetPath              the target directory
  * @param overwrite               whether or not overwrite the file if the target destination already exists.
  * @param createParentDirectories whether or not to attempt creating any parent directories which doesn't exist.
  * @param renameTo                the new file name, {@code null} if the file doesn't need to be renamed
  * @throws IllegalArgumentException if an illegal combination of arguments is supplied
  */
  void copy(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories,
            String renameTo);

  /**
   * Moves the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentDirectory} argument. If such argument is {@false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the moved file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be moved to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be moved
   * recursively
   *
   * @param config                  the config that is parameterizing this operation
   * @param sourcePath              the path to the file to be copied
   * @param targetPath              the target directory
   * @param overwrite               whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param renameTo                the new file name, {@code null} if the file doesn't need to be renamed
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories,
            String renameTo);

  /**
   * Deletes the file pointed by {@code filePath}, provided that it's not locked
   *
   * @param filePath the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exist or is locked
   */
  void delete(String filePath);

  /**
   * Renames the file pointed by {@code filePath} to the provided {@code newName}
   *
   * @param filePath  the path to the file to be renamed
   * @param newName   the file's new name
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   */
  void rename(String filePath, String newName, boolean overwrite);

  /**
   * Creates a new directory
   *
   * @param directoryPath the new directory's path
   */
  void createDirectory(String directoryPath);

  /**
   * Acquires and returns lock over the given {@code path}.
   * <p>
   * Depending on the underlying filesystem, the extent of the lock will depend on the implementation. If a lock can not be
   * acquired, then an {@link IllegalStateException} is thrown.
   * <p>
   * Whoever request the lock <b>MUST</b> release it as soon as possible.
   *
   * @param path   the path to the file you want to lock
   * @return an acquired {@link PathLock}
   * @throws IllegalArgumentException if a lock could not be acquired
   */
  PathLock lock(Path path);

  Lock createMuleLock(String id);

  /**
   * Creates a new {@link DataType} to be associated with a {@link Message} which payload is a {@link InputStream} and the
   * attributes an instance of {@link FtpFileAttributes}
   * <p>
   * It will try to update the {@link DataType#getMediaType()} with a best guess derived from the given {@code attributes}. If no
   * best-guess is possible, then the {@code originalDataType}'s mimeType is honoured.
   * <p>
   * As for the {@link MediaType#getCharset()}, the {@code dataType} one is respected
   *
   * @param attributes        the {@link FtpFileAttributes} of the file being processed
   * @return a {@link DataType} the resulting {@link DataType}.
   */
  MediaType getFileMessageMediaType(FtpFileAttributes attributes);

  /**
   * Verify that the given {@code path} is not locked
   *
   * @param path the path to test
   * @throws IllegalStateException if the {@code path} is indeed locked
   */
  void verifyNotLocked(Path path);

  /**
   * Changes the current working directory to the user base
   */
  void changeToBaseDir();

  String getBasePath();

  /**
   * Retrieves the command responsible for listing files within the file system.
   *
   * <p>
   * The returned {@link ListCommand} allows the execution of file listing operations
   * under the considerations of {@link FileSystem#list(FileConnectorConfig, String, boolean, Predicate)}.
   * It provides methods to list files in a specified directory, optionally including subdirectories,
   * and filter the files based on a provided {@link Predicate}.
   * </p>
   *
   * <p>
   * There are two default implementations provided by the {@link ListCommand} interface:
   * </p>
   *
   * <ul>
   *   <li>{@link ListCommand#list(FileConnectorConfig, String, boolean, Predicate)}:
   *   Lists files in the specified directory path, optionally including subdirectories, and filters the output using the provided {@link Predicate}.</li>
   *
   *   <li>{@link ListCommand#list(FileConnectorConfig, String, boolean, Predicate, SubsetList)}:
   *   Similar to the previous method but also allows obtaining a subset of the results using a {@link SubsetList}.</li>
   * </ul>
   *
   * @return a {@link ListCommand} that can be used to list files in the file system.
   */
  ListCommand getListCommand();

  /**
   * Retrieves the command responsible for reading files from the file system.
   *
   * <p>
   * The returned {@link ReadCommand} enables the execution of file reading operations
   * under the considerations of {@link FileSystem#read(FileConnectorConfig, String, boolean, Long)}.
   * This command provides methods to read files with options for locking the file during the read operation
   * and waiting between size checks to determine if the file is ready to be read.
   * </p>
   *
   * <p>
   * There are two default implementations provided by the {@link ReadCommand} interface:
   * </p>
   *
   * <ul>
   *   <li>{@link ReadCommand#read(FileConnectorConfig, String, boolean, Long)}:
   *   Reads the file at the specified path, optionally locking the file and checking its size at intervals before reading.</li>
   *
   *   <li>{@link ReadCommand#read(FileConnectorConfig, FtpFileAttributes, boolean, Long)}:
   *   Similar to the previous method, but uses pre-collected file attributes to avoid redundant processing, which is particularly useful for remote servers.</li>
   * </ul>
   *
   * @return a {@link ReadCommand} that can be used to read files from the file system.
   */
  ReadCommand getReadCommand();

  /**
   * Retrieves the command responsible for writing files to the file system.
   *
   * <p>
   * The returned {@link WriteCommand} facilitates file writing operations under the
   * considerations of {@link FileSystem#write(String, InputStream, FileWriteMode, boolean, boolean)}.
   * This command allows you to write content to a specified file path with options for
   * setting the write mode, locking the file during the write operation, and optionally
   * creating the parent directory if it doesn't exist.
   * </p>
   *
   * <p>
   * The {@link WriteCommand} interface provides a default implementation for writing a file with
   * the following parameters:
   * </p>
   *
   * <ul>
   *   <li>{@code filePath}: The path where the file will be written.</li>
   *   <li>{@code content}: The {@link InputStream} containing the content to be written into the file.</li>
   *   <li>{@code mode}: The {@link FileWriteMode} determining how the file will be written (e.g., overwrite or append).</li>
   *   <li>{@code lock}: A boolean indicating whether or not to lock the file during the write operation.</li>
   *   <li>{@code createParentDirectory}: A boolean indicating whether or not to attempt creating the parent directory if it doesn't exist.</li>
   * </ul>
   *
   * <p>
   * If an illegal combination of arguments is provided, the {@link WriteCommand#write(String, InputStream, FileWriteMode, boolean, boolean)}
   * method will throw an {@link IllegalArgumentException}.
   * </p>
   *
   * @return a {@link WriteCommand} that can be used to write files to the file system.
   */
  WriteCommand getWriteCommand();

  /**
   * Retrieves the command responsible for copying files within the file system.
   *
   * <p>
   * The returned {@link CopyCommand} allows for performing file copy operations under the
   * considerations of {@link FileSystem#copy(FileConnectorConfig, String, String, boolean, boolean, String)}.
   * This command is used to copy a file from a source path to a target directory, with options for
   * overwriting existing files, creating parent directories, and optionally renaming the file.
   * </p>
   *
   * <p>
   * The {@link CopyCommand} interface provides a method for executing a copy operation with the following parameters:
   * </p>
   *
   * <ul>
   *   <li>{@code config}: The configuration that parameterizes the copy operation.</li>
   *   <li>{@code sourcePath}: The path of the file that needs to be copied.</li>
   *   <li>{@code targetPath}: The path of the target directory where the file will be copied.</li>
   *   <li>{@code overwrite}: A boolean indicating whether to overwrite the file if it already exists at the target destination.</li>
   *   <li>{@code createParentDirectories}: A boolean indicating whether to attempt creating any non-existent parent directories.</li>
   *   <li>{@code renameTo}: The new name for the copied file. If this is {@code null}, the file will retain its original name.</li>
   * </ul>
   *
   * <p>
   * If an illegal combination of arguments is provided, the {@link CopyCommand#copy(FileConnectorConfig, String, String, boolean, boolean, String)}
   * method will throw an {@link IllegalArgumentException}.
   * </p>
   *
   * @return a {@link CopyCommand} that can be used to copy files within the file system.
   */
  CopyCommand getCopyCommand();

  /**
   * Retrieves the command responsible for moving files within the file system.
   *
   * <p>
   * The returned {@link MoveCommand} facilitates file move operations under the
   * considerations of {@link FileSystem#move(FileConnectorConfig, String, String, boolean, boolean, String)}.
   * This command is used to move a file from a source path to a target directory, with options for
   * overwriting existing files, creating parent directories, and optionally renaming the file during the move.
   * </p>
   *
   * <p>
   * The {@link MoveCommand} interface provides a method for executing a move operation with the following parameters:
   * </p>
   *
   * <ul>
   *   <li>{@code config}: The configuration that parameterizes the move operation.</li>
   *   <li>{@code sourcePath}: The path of the file that needs to be moved.</li>
   *   <li>{@code targetPath}: The path of the target directory where the file will be moved.</li>
   *   <li>{@code overwrite}: A boolean indicating whether to overwrite the file if it already exists at the target destination.</li>
   *   <li>{@code createParentDirectories}: A boolean indicating whether to attempt creating any non-existent parent directories.</li>
   *   <li>{@code renameTo}: The new name for the moved file. If this is {@code null}, the file will retain its original name.</li>
   * </ul>
   *
   * <p>
   * If an illegal combination of arguments is provided, the {@link MoveCommand#move(FileConnectorConfig, String, String, boolean, boolean, String)}
   * method will throw an {@link IllegalArgumentException}.
   * </p>
   *
   * @return a {@link MoveCommand} that can be used to move files within the file system.
   */
  MoveCommand getMoveCommand();

  /**
   * Retrieves the command responsible for deleting files within the file system.
   *
   * <p>
   * The returned {@link DeleteCommand} facilitates file deletion operations under the
   * considerations of {@link FileSystem#delete(String)}.
   * This command is used to delete a file at a specified path within the file system.
   * </p>
   *
   * <p>
   * The {@link DeleteCommand} interface provides a method for executing a delete operation with the following parameter:
   * </p>
   *
   * <ul>
   *   <li>{@code filePath}: The path of the file that needs to be deleted.</li>
   * </ul>
   *
   * <p>
   * If the specified file path does not exist or is locked, the {@link DeleteCommand#delete(String)} method will throw an
   * {@link IllegalArgumentException}.
   * </p>
   *
   * @return a {@link DeleteCommand} that can be used to delete files within the file system.
   */
  DeleteCommand getDeleteCommand();


  /**
   * Retrieves the command responsible for renaming files within the file system.
   *
   * <p>
   * The returned {@link RenameCommand} facilitates file renaming operations under the
   * considerations of {@link FileSystem#rename(String, String, boolean)}.
   * This command is used to rename a file located at the specified path within the file system.
   * </p>
   *
   * <p>
   * The {@link RenameCommand} interface provides a method for executing a rename operation with the following parameters:
   * </p>
   *
   * <ul>
   *   <li>{@code filePath}: The path of the file that needs to be renamed.</li>
   *   <li>{@code newName}: The new name for the file.</li>
   *   <li>{@code overwrite}: Whether or not to overwrite the target file if it already exists.</li>
   * </ul>
   *
   * @return a {@link RenameCommand} that can be used to rename files within the file system.
   */
  RenameCommand getRenameCommand();

  /**
   * Retrieves the command responsible for creating directories within the file system.
   *
   * <p>
   * The returned {@link CreateDirectoryCommand} facilitates the creation of new directories under the
   * considerations of {@link FileSystem#createDirectory(String)}. This command is used to create a directory with
   * the specified name within the file system.
   * </p>
   *
   * <p>
   * The {@link CreateDirectoryCommand} interface provides a method for executing the directory creation operation with the following parameter:
   * </p>
   *
   * <ul>
   *   <li>{@code directoryName}: The name of the directory to be created.</li>
   * </ul>
   *
   * @return a {@link CreateDirectoryCommand} that can be used to create directories within the file system.
   */
  CreateDirectoryCommand getCreateDirectoryCommand();

  /**
   * Acquires and returns lock over the given {@code uri}.
   * <p>
   * Depending on the underlying filesystem, the extent of the lock will depend on the implementation. If a lock can not be
   * acquired, then an {@link IllegalStateException} is thrown.
   * <p>
   * Whoever request the lock <b>MUST</b> release it as soon as possible.
   *
   * @param uri   the uri to the file you want to lock
   * @return an acquired {@link UriLock}
   * @throws IllegalArgumentException if a lock could not be acquired
   */
  UriLock lock(URI uri);

  /**
   * Verify that the given {@code uri} is not locked
   *
   * @param uri the uri to test
   * @throws IllegalStateException if the {@code uri} is indeed locked
   */
  void verifyNotLocked(URI uri);
}
