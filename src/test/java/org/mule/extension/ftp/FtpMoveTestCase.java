/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;

import io.qameta.allure.Feature;

@Feature(FTP_EXTENSION)

public class FtpMoveTestCase extends FtpCopyTestCase {

  @Override
  protected String getConfigFile() {
    return "ftp-move-config.xml";
  }

  @Override
  protected String getFlowName() {
    return "move";
  }

  @Override
  protected void assertCopy(String target) throws Exception {
    super.assertCopy(target);
    assertThat(testHarness.fileExists(sourcePath), is(false));
  }
}
