/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mule.extension.ftp.internal.error.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.ftp.internal.util.UriUtils.createUri;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;

import org.junit.Ignore;
import org.mule.extension.ftp.internal.exception.FileAlreadyExistsException;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(FTP_EXTENSION)
@Ignore
public class FtpCreateDirectoryTestCase extends CommonFtpConnectorTestCase {

  private static final String DIRECTORY = "validDirectory";
  private static final String ROOT_CHILD_DIRECTORY = "rootChildDirectory";

  @Override
  protected String getConfigFile() {
    return "ftp-create-directory-config.xml";
  }

  @Test
  public void createDirectory() throws Exception {
    doCreateDirectory(DIRECTORY);
    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  @Test
  public void createExistingDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    final String directory = "washerefirst";
    testHarness.makeDir(directory);
    doCreateDirectory(directory);
  }

  @Test
  public void createDirectoryWithComplexPath() throws Exception {
    String complexPath = createUri(testHarness.getWorkingDirectory(), DIRECTORY).getPath();
    doCreateDirectory(complexPath);

    assertThat(testHarness.dirExists(DIRECTORY), is(true));
  }

  @Test
  public void createDirectoryFromRoot() throws Exception {
    String rootChildDirectoryPath = normalizePath(createUri(testHarness.getWorkingDirectory(), ROOT_CHILD_DIRECTORY).getPath());
    doCreateDirectory(rootChildDirectoryPath);
    assertThat(testHarness.dirExists(rootChildDirectoryPath), is(true));
  }

  @Test
  public void createRootDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("/");
  }

  @Test
  public void createRootCurrentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("/.");
  }

  @Test
  public void createRootParentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("/..");
  }

  @Test
  public void createCurrentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory(".");
  }

  @Test
  public void createParentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("..");
  }

  @Test
  public void createParentParentDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("../..");
  }

  @Test
  public void createDirectoryTwice() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("zarasa/..");
  }

  @Test
  public void createCurrentDirectoryWithNonExistingParent() throws Exception {
    doCreateDirectory("zarasa/.");
    assertThat(testHarness.dirExists("zarasa"), is(true));
  }

  @Test
  public void createDirectoryEndingInSlash() throws Exception {
    doCreateDirectory("zarasa/");
    assertThat(testHarness.dirExists("zarasa"), is(true));
  }

  @Test
  public void createBlankDirectory() throws Exception {
    testHarness.expectedError().expectErrorType("FTP", "ILLEGAL_PATH");
    testHarness.expectedError().expectMessage(containsString("directory path cannot be null nor blank"));
    doCreateDirectory("");
  }

  @Test
  public void createDirectoryWithSpace() throws Exception {
    testHarness.expectedError().expectErrorType("FTP", "ILLEGAL_PATH");
    testHarness.expectedError().expectMessage(containsString("directory path cannot be null nor blank"));
    doCreateDirectory(" ");
  }

  @Test
  public void createComplexDirectoryWithSpace() throws Exception {
    testHarness.expectedError().expectErrorType("MULE", "UNKNOWN");
    testHarness.expectedError()
        .expectMessage(containsString("Found exception trying to recursively create directory /base/zarasa/ /valid"));
    doCreateDirectory("zarasa/ /valid");
  }

  @Test
  public void createDirectoryWithSpaceAndSlash() throws Exception {
    doCreateDirectory(" /");
    assertThat(testHarness.dirExists(" "), is(true));
  }

  @Test
  public void createDirectoryWithSpecialCharacter() throws Exception {
    doCreateDirectory("@");
    assertThat(testHarness.dirExists("@"), is(true));
  }

  @Test
  public void createCurrentDirectoryAndChildDirectoryIgnoresDot() throws Exception {
    doCreateDirectory("./valid");
    assertThat(testHarness.dirExists("valid"), is(true));
  }

  @Test
  public void createParentDirectoryAndChildDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    doCreateDirectory("../valid");
  }

  @Test
  public void createDirectoryStartingWithSlashCreatesAbsoluteDirectory() throws Exception {
    doCreateDirectory("/secondBase/child");
    assertThat(testHarness.dirExists("/secondBase/child"), is(true));
    assertThat(testHarness.dirExists("/base/secondBase/child"), is(false));
  }

  @Test
  public void createRelativeDirectoryResolvesCorrectly() throws Exception {
    testHarness.makeDir("child");
    doCreateDirectory("child/secondChild");
    assertThat(testHarness.dirExists("/base/child/secondChild"), is(true));
    assertThat(testHarness.dirExists("/base/child/child/secondChild"), is(false));
    assertThat(testHarness.dirExists("/base/child/child"), is(false));
  }

  @Test
  public void createDirectoryWithColon() throws Exception {
    final String path = "pathWith:Colon";
    doCreateDirectory(path);
    assertThat(testHarness.dirExists("/base/pathWith:Colon"), is(true));
  }

  @Test
  public void createDirectoryWithGreaterThan() throws Exception {
    final String path = "pathWith>";
    doCreateDirectory(path);
    assertThat(testHarness.dirExists("/base/pathWith>"), is(true));
  }

  private void doCreateDirectory(String directory) throws Exception {
    flowRunner("createDirectory").withVariable("directory", directory).run();
  }
}
