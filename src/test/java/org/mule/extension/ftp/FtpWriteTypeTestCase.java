/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ftp.api.FileTestHarness.HELLO_WORLD;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;

import org.junit.Ignore;
import org.mule.extension.ftp.api.FileWriteMode;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import io.qameta.allure.Feature;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@Feature(FTP_EXTENSION)
@Ignore

public class FtpWriteTypeTestCase extends CommonFtpConnectorTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Ftp - String", HELLO_WORLD, HELLO_WORLD},
        {"Ftp - native byte", "A".getBytes()[0], "A"},
        {"Ftp - Object byte", new Byte("A".getBytes()[0]), "A"},
        {"Ftp - OutputHandler", new TestOutputHandler(), HELLO_WORLD},
        {"Ftp - InputStream", new ByteArrayInputStream(HELLO_WORLD.getBytes()), HELLO_WORLD}});
  }

  private final String name;
  private final Object content;
  private final String expected;
  private String path;

  public FtpWriteTypeTestCase(String name, Object content, String expected) {
    this.name = name;
    this.content = content;
    this.expected = expected;
  }

  @Override
  protected String getConfigFile() {
    return "ftp-write-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    final String folder = "test";
    testHarness.makeDir(folder);
    path = folder + "/test.txt";
  }

  @Test
  public void writeAndAssert() throws Exception {
    write(content);
    assertThat(readPathAsString(path), equalTo(expected));
  }

  private void write(Object content) throws Exception {
    doWrite(path, content, FileWriteMode.APPEND, false);
  }

  private static class TestOutputHandler implements OutputHandler {

    @Override
    public void write(CoreEvent event, OutputStream out) throws IOException {
      org.apache.commons.io.IOUtils.write(HELLO_WORLD, out);
    }
  }

}
