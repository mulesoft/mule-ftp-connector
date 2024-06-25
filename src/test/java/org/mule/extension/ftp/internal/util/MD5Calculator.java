/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.util;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class MD5Calculator {

  public static String calculateMD5(InputStream data) throws IOException {
    return md5Hex(data);
  }

}
