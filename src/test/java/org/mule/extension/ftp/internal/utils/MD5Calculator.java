/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.utils;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class MD5Calculator {

  public static String calculateMD5(InputStream data) throws IOException {
    return md5Hex(data);
  }

}
