/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.bos.dr.service.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Common ZIP operations.
 */
public abstract class ZIP { //NOSONAR

    private ZIP() {}

    /**
     * Read zip entry to byte array.
     * @param zis zip inputstream
     * @return byte[]
     * @throws IOException if IO error reading zip entry
     */
    public static byte[] readEntry(final ZipInputStream zis) throws IOException {
        final var buffer = new byte[1024];
        int len;
        final var os = new ByteArrayOutputStream(1024);
        while ((len = zis.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
        os.close();
        return os.toByteArray();
    }

    /**
     * Write entry to Zip archive.
     * @param path the zip file path
     * @param contents the zip file contents
     * @param zos the zip outputstream
     * @throws IOException if error writing contents to the zip archive
     */
    public static void writeEntry(final String path, byte[] contents, ZipOutputStream zos) throws IOException {
        final var zipEntry = new ZipEntry(path);
        zos.putNextEntry(zipEntry);
        zos.write(contents);
    }
}
