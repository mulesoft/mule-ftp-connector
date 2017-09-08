/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mule.extension.FtpTestHarness.HELLO_PATH;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.extension.sftp.SftpTestHarness.AuthType.PUBLIC_KEY;
import static org.mule.extension.sftp.SftpTestHarness.AuthType.USER_PASSWORD;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.sftp.SftpTestHarness;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;
import io.qameta.allure.Feature;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
@Feature(FTP_EXTENSION)
public abstract class CommonFtpConnectorTestCase extends AbstractFtpConnectorTestCase {

  public static final String FTP_CONNECTION_CONFIG_XML = "ftp-connection-config.xml";
  public static final String SFTP_CONNECTION_XML = "sftp-connection.xml";
  public static final String SFTP_CONNECTION_XML_WITH_IDENTITY_FILE = "sftp-connection-with-identity-file.xml";

  protected static final String NAMESPACE = "FTP";
  private final String name;

  @Rule
  public final FtpTestHarness testHarness;

  @Rule
  public SystemProperty ftpConfigFile;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"ftp", new ClassicFtpTestHarness(), FTP_CONNECTION_CONFIG_XML},
        {"sftp-user-password", new SftpTestHarness(USER_PASSWORD), SFTP_CONNECTION_XML},
        {"sftp-public-key", new SftpTestHarness(PUBLIC_KEY), SFTP_CONNECTION_XML_WITH_IDENTITY_FILE}});
  }

  public CommonFtpConnectorTestCase(String name, FtpTestHarness testHarness, String configName) {
    this.name = name;
    this.testHarness = testHarness;
    this.ftpConfigFile = new SystemProperty("ftp.connection.config", configName);
  }

  protected BaseEvent readHelloWorld() throws Exception {
    return getPath(HELLO_PATH);
  }

  protected Message readPath(String path) throws Exception {
    return readPath(path, true);
  }

  protected Message readPath(String path, boolean streaming) throws Exception {
    return getPath(path, streaming).getMessage();
  }

  protected void doWrite(String path, Object content, FileWriteMode mode, boolean createParent) throws Exception {
    doWrite("write", path, content, mode, createParent);
  }

  protected void doWrite(String flow, String path, Object content, FileWriteMode mode, boolean createParent) throws Exception {
    doWrite(flow, path, content, mode, createParent, null);
  }

  protected void doWrite(String flow, String path, Object content, FileWriteMode mode, boolean createParent, String encoding)
      throws Exception {
    flowRunner(flow).withVariable("path", path).withVariable("createParent", createParent).withVariable("mode", mode)
        .withVariable("encoding", encoding).withPayload(content).run();
  }

  private BaseEvent getPath(String path) throws Exception {
    return getPath(path, true);
  }

  private BaseEvent getPath(String path, boolean streaming) throws Exception {
    return flowRunner("read")
        .withVariable("path", path)
        .withVariable("streaming", streaming)
        .run();
  }

  protected String readPathAsString(String path) throws Exception {
    return toString(readPath(path).getPayload().getValue());
  }

  protected boolean isLocked(Message message) {
    return ((AbstractFileInputStream) message.getPayload().getValue()).isLocked();
  }

  protected String toString(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Message) {
      value = ((Message) value).getPayload().getValue();
    }

    if (value instanceof String) {
      return (String) value;
    }

    InputStream inputStream;
    if (value instanceof CursorStreamProvider) {
      inputStream = ((CursorStreamProvider) value).openCursor();
    } else if (value instanceof InputStream) {
      inputStream = (InputStream) value;
    } else {
      throw new IllegalArgumentException("Result was not of expected type");
    }

    try {
      return IOUtils.toString(inputStream);
    } finally {
      closeQuietly(inputStream);
    }
  }

}
