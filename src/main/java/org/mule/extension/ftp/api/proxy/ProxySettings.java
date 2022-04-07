/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api.proxy;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.tls.TlsContextFactory;

public interface ProxySettings extends Initialisable {

  public String getHost();

  public int getPort();

  public String getUsername();

  public String getPassword();

  public TlsContextFactory getTlsContextFactory();
}
