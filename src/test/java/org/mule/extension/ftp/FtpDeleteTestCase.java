/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ftp.api.FileTestHarness.HELLO_PATH;
import static org.mule.extension.ftp.api.FileTestHarness.HELLO_WORLD;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpDeleteTestCase extends CommonFtpConnectorTestCase {

  private static final String SUB_FOLDER = "files/subfolder";
  private static final String SUB_FOLDER_FILE = "grandChild";
  private static final String SUB_FOLDER_FILE_PATH = String.format("%s/%s", SUB_FOLDER, SUB_FOLDER_FILE);

  @Override
  protected String getConfigFile() {
    return "ftp-delete-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    testHarness.createHelloWorldFile();
    testHarness.makeDir(SUB_FOLDER);
    testHarness.write(String.format("%s/%s", SUB_FOLDER, SUB_FOLDER_FILE), HELLO_WORLD);

    assertExists(true, HELLO_PATH, SUB_FOLDER, SUB_FOLDER_FILE_PATH);
  }

  @Test
  public void deleteFile() throws Exception {
    assertThat(testHarness.fileExists(HELLO_PATH), is(true));
    doDelete(HELLO_PATH);

    assertThat(testHarness.fileExists(HELLO_PATH), is(false));
  }

  @Test
  public void deleteReadFile() throws Exception {
    assertThat(testHarness.fileExists(HELLO_PATH), is(true));
    flowRunner("delete").withVariable("delete", HELLO_PATH).run();

    assertThat(testHarness.fileExists(HELLO_PATH), is(false));
  }

  @Test
  public void deleteFolder() throws Exception {
    doDelete("files");
    assertExists(false, HELLO_PATH, SUB_FOLDER, SUB_FOLDER_FILE);
  }

  @Test
  public void deleteSubFolder() throws Exception {
    doDelete(SUB_FOLDER);
    assertExists(false, SUB_FOLDER);
  }

  @Test
  public void deleteOnBaseDirParent() throws Exception {
    final String path = ".";
    doDelete(path);
    testHarness.assertDeleted(path);
  }

  private void doDelete(String path) throws Exception {
    flowRunner("delete").withVariable("delete", path).run();
  }

  private void assertExists(boolean exists, String... paths) throws Exception {
    for (String path : paths) {
      assertThat(testHarness.fileExists(path), is(exists));
    }
  }
}
