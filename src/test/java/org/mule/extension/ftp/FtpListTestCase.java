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
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpListTestCase extends CommonFtpConnectorTestCase {

  private static final String TEST_FILE_PATTERN = "test-file-%d.html";
  private static final String SUB_DIRECTORY_NAME = "subDirectory";
  private static final String CONTENT = "foo";
  private static final String LONG_CONTENT = "longcontentlongcontentlongcontentlongcontent";
  private static final String LONG_CONTENT_FILE_NAME = "longContent.txt";
  private static int WRITE_DELAY = 1000;


  @Override
  protected String getConfigFile() {
    return "ftp-list-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    TestProcessor.clear();
    createTestFiles();
    FilesWrittenProcessor.clear();
    FilesBeingWrittenProcessor.clear();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    TestProcessor.clear();
    FilesWrittenProcessor.clear();
    FilesBeingWrittenProcessor.clear();
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
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exist");
    doList("list", "whatever", false);
  }

  @Test
  public void listNotRecursiveWithSizeCheck() throws Exception {
    List<Message> messages = doListWithStableSizeCheck(".", false);

    assertThat(messages, hasSize(6));
    assertThat(assertListedFiles(messages), is(true));
  }

  @Test
  public void listRecursiveWithSizeCheck() throws Exception {
    List<Message> messages = doListWithStableSizeCheck(".", true);

    assertThat(messages, hasSize(8));
    assertThat(assertListedFiles(messages), is(true));

    List<Message> subDirectories = messages.stream()
        .filter(message -> ((FileAttributes) message.getAttributes().getValue()).isDirectory())
        .collect(toList());

    assertThat(subDirectories, hasSize(1));
    assertThat(assertListedFiles(subDirectories), is(true));
  }

  @Test
  public void notDirectoryWithSizeCheck() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class,
                                            "Only directories can be listed");
    doList("listWithStableSizeTime", format(TEST_FILE_PATTERN, 0), false);
  }

  @Test
  public void notExistingPathWithSizeCheck() throws Exception {
    testHarness.expectedError().expectError(NAMESPACE, ILLEGAL_PATH.getType(), IllegalPathException.class, "doesn't exist");
    doList("listWithStableSizeTime", "whatever", false);
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

  @Test
  public void listFilesRecursivelyWithNameFilter() throws Exception {
    List<Message> messages = doList("listFilesRecursivelyWithNameFilter", ".", true);
    assertThat(messages, hasSize(7));
  }

  @Test
  public void listFilesRecursivelyWithSpecificNameFilter() throws Exception {
    List<Message> messages = doList("listFilesRecursivelyWithSpecificNameFilter", ".", true);
    assertThat(messages, hasSize(1));
  }

  @Test
  public void listFilesWithFilesStillBeingWritten() throws Exception {
    testHarness.writeByteByByteAsync(LONG_CONTENT_FILE_NAME, LONG_CONTENT, WRITE_DELAY);
    List<Message> messages = doList("listFilesWithFilesStillBeingWritten", ".", true);
    assertThat(FilesBeingWrittenProcessor.getFilePaths(), hasSize(1));
    assertThat(FilesWrittenProcessor.getFilePaths(), hasSize(8));
    assertThat(messages, hasSize(8));
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

  private List<Message> doListWithStableSizeCheck(String path, boolean recursive)
      throws Exception {
    return doList("listWithStableSizeTime", path, recursive);
  }

  private List<Message> doList(String flowName, String path, boolean recursive) throws Exception {
    CursorIteratorProvider iteratorProvider = (CursorIteratorProvider) (flowRunner(flowName)
        .withVariable("path", path).withVariable("recursive", recursive).keepStreamsOpen()
        .run()
        .getMessage().getPayload().getValue());

    assertThat(iteratorProvider, is(notNullValue()));

    Iterator<Message> iterator = iteratorProvider.openCursor();

    List<Message> results = new LinkedList<>();

    while (iterator.hasNext()) {
      results.add(iterator.next());
    }

    return results;
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
    public CoreEvent process(CoreEvent event) throws MuleException {
      CursorIteratorProvider iteratorProvider = (CursorIteratorProvider) event.getMessage().getPayload().getValue();
      Iterator<Message> iterator = iteratorProvider.openCursor();
      List<Message> messageList = new LinkedList<>();

      while (iterator.hasNext()) {
        messageList.add(iterator.next());
      }

      for (Message message : messageList) {
        InputStream inputStream = (InputStream) ((CursorProvider) message.getPayload().getValue()).openCursor();
        fileContents.add(new String(toByteArray(inputStream)));
      }

      return event;
    }

  }

  public static class FilesWrittenProcessor implements Processor {

    private static ArrayList<String> filePaths = new ArrayList<>();

    static ArrayList<String> getFilePaths() {
      return filePaths;
    }

    static void clear() {
      filePaths.clear();
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      filePaths.add(((FtpFileAttributes) event.getMessage().getAttributes().getValue()).getPath());
      return event;
    }
  }

  public static class FilesBeingWrittenProcessor implements Processor {

    private static ArrayList<String> filePaths = new ArrayList<>();

    static ArrayList<String> getFilePaths() {
      return filePaths;
    }

    static void clear() {
      filePaths.clear();
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      filePaths.add(((FtpFileAttributes) event.getMessage().getAttributes().getValue()).getPath());
      return event;
    }
  }

}
