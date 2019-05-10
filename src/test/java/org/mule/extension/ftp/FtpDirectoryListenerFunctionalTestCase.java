/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ftp.AllureConstants.FtpFeature.FTP_EXTENSION;
import static org.mule.extension.ftp.internal.FtpUtils.normalizePath;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.File;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(FTP_EXTENSION)
public class FtpDirectoryListenerFunctionalTestCase extends CommonFtpConnectorTestCase {

  private static final String MATCHERLESS_LISTENER_FOLDER_NAME = "matcherless";
  private static final String SHARED_LISTENER_FOLDER_NAME = "shared";
  private static final String WITH_MATCHER_FOLDER_NAME = "withMatcher";
  private static final String WATCH_FILE = "watchme.txt";
  private static final String DEFAULT_FILE = "default.txt";
  private static final String WATCH_CONTENT = "who watches the watchmen?";
  private static final String DR_MANHATTAN = "Dr. Manhattan";
  private static final String MATCH_FILE = "matchme.txt";
  private static final String DEFAULT_DIRECTORY_CONTENT = "defaultdefaultdefault";
  private static final int PROBER_TIMEOUT = 10000;
  private static final int PROBER_DELAY = 1000;

  private static List<Message> RECEIVED_MESSAGES;


  public static class TestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      RECEIVED_MESSAGES.add(event.getMessage());
      return event;
    }
  }

  @Override
  protected String getConfigFile() {
    return "ftp-directory-listener-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    testHarness.makeDir(MATCHERLESS_LISTENER_FOLDER_NAME);
    testHarness.makeDir(WITH_MATCHER_FOLDER_NAME);
    testHarness.makeDir(SHARED_LISTENER_FOLDER_NAME);

    RECEIVED_MESSAGES = new CopyOnWriteArrayList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    RECEIVED_MESSAGES = null;
  }

  @Test
  @Description("Verifies that a created file is picked")
  public void onFileCreated() throws Exception {
    File file = new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);
    assertPoll(file, WATCH_CONTENT);
  }

  @Test
  @Description("Verifies that a created file in the default directory is picked")
  public void onFileCreatedOnDefaultDirectory() throws Exception {
    File file = new File(DEFAULT_FILE);
    testHarness.write(file.getPath(), DEFAULT_DIRECTORY_CONTENT);
    assertPoll(file, DEFAULT_DIRECTORY_CONTENT);
  }

  @Test
  @Description("Verifies that files created in subdirs are picked")
  public void recursive() throws Exception {
    File subdir = new File(MATCHERLESS_LISTENER_FOLDER_NAME, "subdir");
    testHarness.makeDir(subdir.getPath());
    final File file = new File(subdir, WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);

    assertPoll(file, WATCH_CONTENT);
  }

  @Test
  @Description("Verifies that files created in subdirs are not picked")
  public void nonRecursive() throws Exception {
    stopFlow("listenWithoutMatcher");

    startFlow("listenNonRecursive");
    File subdir = new File(MATCHERLESS_LISTENER_FOLDER_NAME, "subdir");
    testHarness.makeDir(subdir.getPath());
    File file = new File(subdir, WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);

    expectNot(file);

    file = new File(MATCHERLESS_LISTENER_FOLDER_NAME, "nonRecursive.txt");
    final String nonRecursiveContent = "you shall not recurse";
    testHarness.write(file.getPath(), nonRecursiveContent);
    assertPoll(file, nonRecursiveContent);
  }

  @Test
  @Description("Verifies that only files compliant with the matcher are picked")
  public void matcher() throws Exception {
    final File file = new File(WITH_MATCHER_FOLDER_NAME, MATCH_FILE);
    final File rejectedFile = new File(WITH_MATCHER_FOLDER_NAME, WATCH_FILE);
    testHarness.write(file.getPath(), DR_MANHATTAN);
    testHarness.write(rejectedFile.getPath(), WATCH_CONTENT);

    assertPoll(file, DR_MANHATTAN);
    checkNot(PROBER_TIMEOUT, PROBER_DELAY, () -> RECEIVED_MESSAGES.size() > 1);
  }

  @Test
  @Description("Verifies that files are moved after processing")
  public void moveTo() throws Exception {
    stopFlow("listenWithoutMatcher");
    startFlow("moveTo");

    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files are moved after processing even if autoDelete is configured")
  public void moveToAndAutoDelete() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("moveToAndAutoDelete");

    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files that cannot be moved because a file already exists in the other directory with that name are deleted")
  public void moveToAndAutoDeleteWithSameFileName() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("moveToAndAutoDelete");
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
    RECEIVED_MESSAGES.clear();
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files that cannot be moved because a file already exists in the other directory with that name remain untouched")
  public void moveToWithSameFileName() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("moveTo");
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
    RECEIVED_MESSAGES.clear();
    onFileCreated();
    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
    checkNot(PROBER_TIMEOUT, PROBER_DELAY,
             () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()));
  }

  @Test
  @Description("Verifies that files are moved and renamed after processing")
  public void moveToWithRename() throws Exception {
    stopFlow("listenWithoutMatcher");
    startFlow("moveToWithRename");

    File file = new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE);
    testHarness.write(file.getPath(), WATCH_CONTENT);

    check(PROBER_TIMEOUT, PROBER_DELAY,
          () -> !testHarness.fileExists(new File(MATCHERLESS_LISTENER_FOLDER_NAME, WATCH_FILE).getPath()) &&
              testHarness.fileExists(new File(SHARED_LISTENER_FOLDER_NAME, "renamed.txt").getPath()));
  }

  @Test
  @Description("Tests the case of watermark on update timestamp, processing only files that have been modified after the prior poll")
  public void watermarkForModifiedFiles() throws Exception {
    stopFlow("listenWithoutMatcher");
    stopFlow("redundantListener1");
    stopFlow("redundantListener2");
    stopFlow("listenTxtOnly");

    startFlow("modifiedWatermark");

    final String filePath = MATCHERLESS_LISTENER_FOLDER_NAME + "/" + WATCH_FILE;
    final String filePath2 = MATCHERLESS_LISTENER_FOLDER_NAME + "/" + WATCH_FILE + "2";
    testHarness.write(filePath, WATCH_CONTENT);
    testHarness.write(filePath2, WATCH_CONTENT);

    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      if (RECEIVED_MESSAGES.size() == 2) {
        return RECEIVED_MESSAGES.stream().anyMatch(m -> containsPath(m, filePath)) &&
            RECEIVED_MESSAGES.stream().anyMatch(m -> containsPath(m, filePath2));
      }

      return false;
    });

    assertThat(testHarness.fileExists(filePath), is(true));
    assertThat(testHarness.fileExists(filePath2), is(true));

    Thread.sleep(2000);
    final String modifiedData = "modified!";
    RECEIVED_MESSAGES.clear();
    testHarness.write(filePath, modifiedData);
    testHarness.setTimestamp(filePath, now().plus(1, ChronoUnit.DAYS));

    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      if (RECEIVED_MESSAGES.size() == 1) {
        Message message = RECEIVED_MESSAGES.get(0);
        return containsPath(message, filePath) && message.getPayload().getValue().toString().contains(modifiedData);
      }

      return false;
    });
  }

  private boolean containsPath(Message message, String path) {
    FtpFileAttributes attrs = (FtpFileAttributes) message.getAttributes().getValue();
    return attrs.getPath().contains(path);
  }

  private void assertPoll(File file, Object expectedContent) {
    Message message = expect(file);
    String payload = toString(message.getPayload().getValue());
    assertThat(payload, equalTo(expectedContent));
  }

  private Message expect(File file) {
    Reference<Message> messageHolder = new Reference<>();
    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      getPicked(file).ifPresent(messageHolder::set);
      return messageHolder.get() != null;
    });

    return messageHolder.get();
  }

  private void expectNot(File file) {
    checkNot(PROBER_TIMEOUT, PROBER_DELAY, () -> getPicked(file).isPresent());
  }

  private Optional<Message> getPicked(File file) {
    return RECEIVED_MESSAGES.stream()
        .filter(message -> {
          FileAttributes attributes = (FileAttributes) message.getAttributes().getValue();
          return attributes.getPath().contains(normalizePath(file.getPath()));
        })
        .findFirst();
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) getFlowConstruct(flowName)).stop();
  }
}
