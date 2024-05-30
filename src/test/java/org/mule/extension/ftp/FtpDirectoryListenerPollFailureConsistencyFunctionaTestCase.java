/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.tck.probe.PollingProber.check;

import org.junit.Ignore;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.qameta.allure.Description;
import org.junit.Test;

@Ignore
public class FtpDirectoryListenerPollFailureConsistencyFunctionaTestCase extends CommonFtpConnectorTestCase {

  private static final String CONTENT = "File Content.";
  private static final String FILE_NAME = "file_%s.txt";
  private static final String INPUT_FOLDER = "input";
  private static final int PROBER_TIMEOUT = 100000;
  private static final int PROBER_DELAY = 2000;
  private static final int FILE_CREATION_DELAY_MILLIS = 100;
  private static final int NUMBER_OF_FILES = 100;

  private static Set<String> FILES_PROCESSED;

  @Override
  public int getTestTimeoutSecs() {
    return 300;
  }

  public static class TestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (FILES_PROCESSED) {
        if (FILES_PROCESSED != null) {
          FILES_PROCESSED.add(((FtpFileAttributes) (event.getMessage().getAttributes().getValue())).getName());
        }
        return event;
      }
    }
  }

  @Override
  protected String getConfigFile() {
    return "ftp-directory-listener-with-skipped-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    testHarness.makeDir(INPUT_FOLDER);

    FILES_PROCESSED = ConcurrentHashMap.newKeySet();
  }

  @Override
  protected void doTearDown() throws Exception {
    FILES_PROCESSED = null;
  }

  private URI buildPath(String... path) throws Exception {
    return new URI(String.join("/", path));
  }

  @Test
  @Description("Verifies that a the failure of a poll does not make the next poll iteration skip unprocessed files")
  public void filesAreNotSkippedWithPollFailures() throws Exception {
    createShuffledFilesWithDelay();
    assertAllFilesArePolled();
  }

  private void assertAllFilesArePolled() throws Exception {
    check(PROBER_TIMEOUT, PROBER_DELAY, () -> {
      assertThat(String.join(" , ", FILES_PROCESSED), FILES_PROCESSED, hasSize(100));
      return true;
    });
  }

  private void createShuffledFilesWithDelay() throws Exception {
    List<Integer> numbers = new ArrayList<>();
    for (int i = 1; i <= NUMBER_OF_FILES; i++) {
      numbers.add(i);
    }
    Collections.shuffle(numbers);

    for (Integer i : numbers) {
      URI file = buildPath(INPUT_FOLDER, String.format(FILE_NAME, i + 1));
      testHarness.write(file.getPath(), CONTENT);
      sleep(FILE_CREATION_DELAY_MILLIS);
    }
  }

}
