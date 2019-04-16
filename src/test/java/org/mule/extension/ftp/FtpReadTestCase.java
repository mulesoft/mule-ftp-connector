/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.junit.rules.ExpectedException.none;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.test.extension.file.common.api.FileTestHarness.BINARY_FILE_NAME;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_PATH;
import static org.mule.test.extension.file.common.api.FileTestHarness.HELLO_WORLD;
import org.mule.extension.file.common.api.exceptions.DeletedFileWhileReadException;
import org.mule.extension.file.common.api.exceptions.FileBeingModifiedException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.file.common.api.stream.AbstractFileInputStream;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import io.qameta.allure.Feature;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(FTP_EXTENSION)
public class FtpReadTestCase extends CommonFtpConnectorTestCase {

  private static String DELETED_FILE_NAME = "deleted.txt";
  private static String DELETED_FILE_CONTENT = "non existant content";
  private static String WATCH_FILE = "watch.txt";
  private static String WATCH_SPACES_FILE_NAME = "watch with-spaces.txt";
  private static String WATCH_SPACES_FILE_CONTENT = "Content of the file with spaces in its name.";

  @Override
  protected String getConfigFile() {
    return "ftp-read-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    testHarness.createHelloWorldFile();
  }

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void read() throws Exception {
    Message message = readHelloWorld().getMessage();

    assertThat(message.getPayload().getDataType().getMediaType().getPrimaryType(), is(JSON.getPrimaryType()));
    assertThat(message.getPayload().getDataType().getMediaType().getSubType(), is(JSON.getSubType()));

    assertThat(toString(message.getPayload().getValue()), is(HELLO_WORLD));
  }

  @Test
  public void readBinary() throws Exception {
    testHarness.createBinaryFile();

    Message response = readPath(BINARY_FILE_NAME, false);

    assertThat(response.getPayload().getDataType().getMediaType().getPrimaryType(), is(MediaType.BINARY.getPrimaryType()));
    assertThat(response.getPayload().getDataType().getMediaType().getSubType(), is(MediaType.BINARY.getSubType()));

    InputStream payload = (InputStream) response.getPayload().getValue();

    byte[] readContent = new byte[new Long(HELLO_WORLD.length()).intValue()];
    org.apache.commons.io.IOUtils.read(payload, readContent);
    assertThat(new String(readContent), is(HELLO_WORLD));
  }

  @Test
  public void readWithForcedMimeType() throws Exception {
    CoreEvent event = flowRunner("readWithForcedMimeType").withVariable("path", HELLO_PATH).run();
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(), equalTo("test"));
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getSubType(), equalTo("test"));
  }

  @Test
  public void readUnexisting() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exist");
    readPath("files/not-there.txt");
  }

  @Test
  public void readDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "since it's a directory");
    readPath("files");
  }

  @Test
  public void getProperties() throws Exception {
    FtpFileAttributes fileAttributes = (FtpFileAttributes) readHelloWorld().getMessage().getAttributes().getValue();
    testHarness.assertAttributes(HELLO_PATH, fileAttributes);
  }

  public static class StreamCloserTestMessageProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        assertThat(((AbstractFileInputStream) event.getMessage().getPayload().getValue()).isLocked(), is(true));
        ((InputStream) event.getMessage().getPayload().getValue()).close();
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
      return event;
    }
  }

  @Test
  public void readFileThatIsDeleted() throws Exception {
    expectedException.expectCause(hasCause(instanceOf(DeletedFileWhileReadException.class)));
    expectedException.expectMessage("was read but does not exist anymore.");
    testHarness.write(DELETED_FILE_NAME, DELETED_FILE_CONTENT);
    flowRunner("readFileThatIsDeleted").withVariable("path", DELETED_FILE_NAME).run().getMessage().getPayload().getValue();
  }

  @Test
  public void readWhileStillWriting() throws Exception {
    expectedException.expectCause(hasCause(instanceOf(FileBeingModifiedException.class)));
    expectedException.expectMessage("is still being written");
    testHarness.writeByteByByteAsync(WATCH_FILE, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 500);
    flowRunner("readFileWithSizeCheck").withVariable("path", WATCH_FILE).run().getMessage().getPayload().getValue();
  }

  @Test
  public void readWhileFinishWriting() throws Exception {
    testHarness.writeByteByByteAsync(WATCH_FILE, "aaaaa", 500);
    String result = (String) flowRunner("readFileWithSizeCheck").withVariable("path", WATCH_FILE).run().getMessage()
        .getPayload().getValue();
    assertThat(result, is("aaaaa"));
  }

  @Test
  public void readFileWithSpaceInItsName() throws Exception {
    testHarness.write(WATCH_SPACES_FILE_NAME, WATCH_SPACES_FILE_CONTENT);
    String content = (String) readPath(WATCH_SPACES_FILE_NAME).getPayload().getValue();
    assertThat(content, is(WATCH_SPACES_FILE_CONTENT));
  }

  private Message readWithLock() throws Exception {
    Message message =
        flowRunner("readWithLock").withVariable("readPath", Paths.get("files/hello.json").toString()).run().getMessage();
    return message;
  }
}
