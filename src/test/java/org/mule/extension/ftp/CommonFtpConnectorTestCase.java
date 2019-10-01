/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_PATH;

import org.mule.extension.file.common.api.FileWriteMode;
import org.mule.extension.file.common.api.stream.AbstractNonFinalizableFileInputStream;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.InputStream;

import org.junit.Rule;

import io.qameta.allure.Feature;

@Feature(FTP_EXTENSION)
public abstract class CommonFtpConnectorTestCase extends AbstractFtpConnectorTestCase {

  protected static final String NAMESPACE = "FTP";

  @Rule
  public final DefaultFtpTestHarness testHarness = new DefaultFtpTestHarness();

  @Rule
  public SystemProperty ftpConfigFile = new SystemProperty("ftp.connection.config", "ftp-connection.xml");

  protected CoreEvent readHelloWorld() throws Exception {
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

  private CoreEvent getPath(String path) throws Exception {
    return getPath(path, true);
  }

  private CoreEvent getPath(String path, boolean streaming) throws Exception {
    return flowRunner("read")
        .withVariable("path", path)
        .withVariable("streaming", streaming)
        .run();
  }

  protected String readPathAsString(String path) throws Exception {
    return toString(readPath(path).getPayload().getValue());
  }

  protected boolean isLocked(Message message) {
    return ((AbstractNonFinalizableFileInputStream) message.getPayload().getValue()).isLocked();
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
