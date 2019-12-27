/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.lifecycle;

import org.mule.runtime.core.api.util.FileUtils;
import org.mule.test.infrastructure.server.ftp.EmbeddedFtpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.commons.io.IOUtils;

public class FtpServerLifecycleManager {

  private static final String FTP_SERVER_BASE_DIR = "target/ftpserver";

  private static final String BASE_DIR = "/";

  private static File baseDir;
  private static EmbeddedFtpServer server;

  public static void startFtpServer(String port) throws Exception {
    createAndStartServer(Integer.valueOf(port));
  }

  private static void createAndStartServer(int port) {
    baseDir = new File(FTP_SERVER_BASE_DIR, BASE_DIR);
    try {
      createFtpServerBaseDir();
      server = createServer(port);
      server.start();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static EmbeddedFtpServer createServer(int port) throws Exception {
    return new EmbeddedFtpServer(port);
  }

  private static void createFtpServerBaseDir() {
    deleteFtpServerBaseDir();
    baseDir.mkdirs();
  }

  public static void stopFtpServer() throws Exception {
    stopServer();
    deleteFtpServerBaseDir();
  }

  private static void stopServer() {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        throw new RuntimeException("Could not stop FTPS server", e);
      }
    }
  }

  private static void deleteFtpServerBaseDir() {
    FileUtils.deleteTree(baseDir);
  }

  public static void createEncodedFileName() throws IOException {
    FileInputStream fis = new FileInputStream("src/test/resources/ftp-utf8-filename.txt");
    String filename = IOUtils.toString(fis, StandardCharsets.UTF_8);
    File file = new File(FTP_SERVER_BASE_DIR, filename);
    file.createNewFile();
    Files.write(Paths.get(file.getAbsolutePath()), Collections.singletonList("Content for UTF-8 filename."),
                StandardCharsets.ISO_8859_1);
  }

  public static void cleanServer() {
    createFtpServerBaseDir();
  }
}
