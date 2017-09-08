/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.file.common.api.exceptions.FileError.ILLEGAL_PATH;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import org.mule.extension.FtpTestHarness;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpListTestCase extends CommonFtpConnectorTestCase {

  private static final String TEST_FILE_PATTERN = "test-file-%d.html";
  private static final String SUB_DIRECTORY_NAME = "subDirectory";
  private static final String CONTENT = "foo";

  public FtpListTestCase(String name, FtpTestHarness testHarness, String ftpConfigFile) {
    super(name, testHarness, ftpConfigFile);
  }

  @Override
  protected String getConfigFile() {
    return "ftp-list-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    TestProcessor.clear();
    createTestFiles();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    TestProcessor.clear();
  }

  @Test
  public void listNotRecursive() throws Exception {
    List<Message> messages = doList(".", false);

    assertThat(messages, hasSize(6));
    assertThat(assertListedFiles(messages), is(true));
  }

  @Test
  public void listRecursive() throws Exception {
    List<Message> messages = doList(".", true);

    assertThat(messages, hasSize(8));
    assertThat(assertListedFiles(messages), is(true));

    List<Message> subDirectories = messages.stream()
        .filter(message -> ((FileAttributes) message.getAttributes().getValue()).isDirectory())
        .collect(toList());

    assertThat(subDirectories, hasSize(1));
    assertThat(assertListedFiles(subDirectories), is(true));
  }

  @Test
  public void notDirectory() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "Only directories can be listed");
    doList("list", format(TEST_FILE_PATTERN, 0), false);
  }

  @Test
  public void notExistingPath() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exists");
    doList("list", "whatever", false);
  }

  @Test
  public void listWithEmbeddedMatcher() throws Exception {
    List<Message> messages = doList("listWithEmbeddedPredicate", ".", false);

    assertThat(messages, hasSize(2));
    assertThat(assertListedFiles(messages), is(false));
  }

  @Test
  public void listWithGlobalMatcher() throws Exception {
    List<Message> messages = doList("listWithGlobalMatcher", ".", true);

    assertThat(messages, hasSize(1));

    FileAttributes file = (FileAttributes) messages.get(0).getAttributes().getValue();
    assertThat(file.isDirectory(), is(true));
    assertThat(file.getName(), equalTo(SUB_DIRECTORY_NAME));
  }

  @Test
  public void listTwoOpenCursors() throws Exception {
    List<Message> messages = doList("listCursors", ".", false);

    assertThat(messages, hasSize(1));

    ArrayList<String> contents = TestProcessor.getFileContents();
    assertThat(contents, hasSize(2));
    assertThat(contents.get(0), is(CONTENT));
    assertThat(contents.get(1), is(CONTENT));
  }

  private boolean assertListedFiles(List<Message> messages) throws Exception {
    boolean directoryWasFound = false;

    for (Message message : messages) {
      FileAttributes attributes = (FileAttributes) message.getAttributes().getValue();
      if (attributes.isDirectory()) {
        assertThat("two directories found", directoryWasFound, is(false));
        directoryWasFound = true;
        assertThat(attributes.getName(), equalTo(SUB_DIRECTORY_NAME));
      } else {
        assertThat(attributes.getName(), endsWith(".html"));
        assertThat(toString(message.getPayload().getValue()), equalTo(CONTENT));
        assertThat(attributes.getSize(), is(new Long(CONTENT.length())));
      }
    }

    return directoryWasFound;
  }

  private List<Message> doList(String path, boolean recursive) throws Exception {
    return doList("list", path, recursive);
  }

  private List<Message> doList(String flowName, String path, boolean recursive) throws Exception {
    List<Message> messages =
        (List<Message>) flowRunner(flowName).withVariable("path", path).withVariable("recursive", recursive).run()
            .getMessage().getPayload().getValue();

    assertThat(messages, is(notNullValue()));
    return messages;
  }

  private void createTestFiles() throws Exception {
    createTestFiles(".", 0, 5);
    createSubDirectory();
  }

  private void createSubDirectory() throws Exception {
    testHarness.makeDir(SUB_DIRECTORY_NAME);
    createTestFiles(SUB_DIRECTORY_NAME, 5, 7);
  }

  private void createTestFiles(String parentFolder, int startIndex, int endIndex) throws Exception {
    for (int i = startIndex; i < endIndex; i++) {
      String name = String.format(TEST_FILE_PATTERN, i);
      testHarness.write(parentFolder, name, CONTENT);
    }
  }

  public static class TestProcessor implements Processor {

    private static ArrayList<String> fileContents = new ArrayList<>();

    static ArrayList<String> getFileContents() {
      return fileContents;
    }

    static void clear() {
      fileContents.clear();
    }

    @Override
    public BaseEvent process(BaseEvent event) throws MuleException {
      Collection<Message> messageList = (Collection<Message>) event.getMessage().getPayload().getValue();

      for (Message message : messageList) {
        fileContents.add(new String(toByteArray(((CursorStreamProvider) message.getPayload().getValue()).openCursor())));
      }

      return event;
    }

  }
}
