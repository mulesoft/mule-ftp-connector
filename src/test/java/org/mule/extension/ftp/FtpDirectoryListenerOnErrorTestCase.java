/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FtpDirectoryListenerOnErrorTestCase extends CommonFtpConnectorTestCase {

  private static final String CONTENT = "File Content.";
  private static final String FILE_NAME = "file_%s.txt";
  private static final String INPUT_FOLDER = "input";
  private static final int PROBER_TIMEOUT = 100000;
  private static final int PROBER_DELAY = 2000;
  private static final int FILE_CREATION_DELAY_MILLIS = 100;
  private static final int NUMBER_OF_FILES = 100;
  private static Set<String> FILES_PROCESSED;

  @Override
  protected String getConfigFile() {
    return "ftp-directory-listener-on-error-config.xml";
  }

  private static List<Message> RECEIVED_MESSAGES;


  public static class TestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      RECEIVED_MESSAGES.add(event.getMessage());
      return event;
    }
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

  @Test
  public void whenListenerFailsThenWriteAlsoFailsInErrorHandler() {

  }

}
