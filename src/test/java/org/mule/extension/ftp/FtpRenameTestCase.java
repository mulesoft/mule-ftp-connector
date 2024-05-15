/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ftp.internal.util.UriUtils.createUri;
import static org.mule.extension.ftp.internal.util.UriUtils.trimLastFragment;
import static org.mule.extension.ftp.api.FileTestHarness.HELLO_FILE_NAME;
import static org.mule.extension.ftp.api.FileTestHarness.HELLO_PATH;
import static org.mule.extension.ftp.api.FileTestHarness.HELLO_WORLD;
import static org.mule.extension.ftp.internal.error.FileError.FILE_ALREADY_EXISTS;
import static org.mule.extension.ftp.internal.error.FileError.ILLEGAL_PATH;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import org.mule.extension.ftp.internal.exception.FileAlreadyExistsException;
import org.mule.extension.ftp.internal.exception.IllegalPathException;

import java.net.URI;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpRenameTestCase extends CommonFtpConnectorTestCase {

  private static final String RENAME_TO = "renamed";

  private URI uri = createUri(HELLO_PATH);

  @Override
  protected String getConfigFile() {
    return "ftp-rename-config.xml";
  }

  @Test
  public void renameFile() throws Exception {
    testHarness.createHelloWorldFile();
    doRename(HELLO_PATH);
    assertRenamedFile();
  }

  @Test
  public void renameReadFile() throws Exception {
    testHarness.createHelloWorldFile();
    doRename("readAndRename", HELLO_PATH, RENAME_TO, false);
    assertRenamedFile();
  }

  @Test
  public void renameDirectory() throws Exception {
    testHarness.createHelloWorldFile();
    final String sourcePath = trimLastFragment(uri).getPath();
    doRename(sourcePath);

    assertThat(testHarness.dirExists(sourcePath), is(false));
    assertThat(testHarness.dirExists(RENAME_TO), is(true));

    assertThat(readPathAsString(String.format("%s/%s", RENAME_TO, HELLO_FILE_NAME)), is(HELLO_WORLD));
  }

  @Test
  public void renameUnexisting() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exist");
    doRename("not-there.txt");
  }

  @Test
  public void targetPathContainsParts() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "parameter of rename operation should not contain any file separator character");
    testHarness.createHelloWorldFile();
    final String sourcePath = trimLastFragment(uri).getPath();
    doRename("rename", sourcePath, "path/with/parts", true);
  }

  @Test
  public void targetAlreadyExistsWithoutOverwrite() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, FILE_ALREADY_EXISTS.getType(), FileAlreadyExistsException.class,
                                            "already exists");
    final String sourceFile = "renameme.txt";
    testHarness.write(sourceFile, "rename me");
    testHarness.write(RENAME_TO, "I was here first");

    doRename(sourceFile);
  }

  @Test
  public void targetAlreadyExistsWithOverwrite() throws Exception {
    testHarness.createHelloWorldFile();
    final String sourcePath = createUri(trimLastFragment(uri).getPath(), RENAME_TO).getPath();
    testHarness.write(sourcePath, "I was here first");

    doRename(HELLO_PATH, true);
    assertRenamedFile();
  }

  private void assertRenamedFile() throws Exception {
    final URI parentUri = trimLastFragment(createUri(testHarness.getWorkingDirectory(), HELLO_PATH));
    final String targetUri = createUri(parentUri.getPath(), RENAME_TO).getPath();

    assertThat(testHarness.fileExists(targetUri), is((true)));
    assertThat(testHarness.fileExists(HELLO_PATH), is((false)));
    assertThat(readPathAsString(targetUri), is(HELLO_WORLD));
  }

  private void doRename(String source) throws Exception {
    doRename("rename", source, RENAME_TO, false);
  }

  private void doRename(String source, boolean overwrite) throws Exception {
    doRename("rename", source, RENAME_TO, overwrite);
  }

  private void doRename(String flow, String source, String to, boolean overwrite) throws Exception {
    flowRunner(flow).withVariable("path", source).withVariable("to", to).withVariable("overwrite", overwrite).run();
  }
}
