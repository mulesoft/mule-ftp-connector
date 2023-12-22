package org.mule.extension.ftp.internal.connection;

/**
 * Flag to indicate if the FTP server supports the using initiateListParsing to list a single file.
 */
public enum SingleFileListingMode {
    UNSET, SUPPORTED, UNSUPPORTED
}
