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
package com.ericsson.bos.dr.tests.integration.utils


import com.ericsson.bos.dr.service.utils.ZIP

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class IOUtils {

    static String readClasspathResource(String resource) {
        final InputStream is = IOUtils.class.getResourceAsStream(resource)
        return is == null ? "" : new String(is.readAllBytes())
    }

    static byte[] readClasspathResourceBytes(String resource) {
        final InputStream is = IOUtils.class.getResourceAsStream(resource)
        return is == null ? [] : is.readAllBytes()
    }
    
    static Map<String, String> readZipEntries(final String path) {
        final Map<String, String> entries = new HashMap()
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(path))) {
            ZipEntry ze
            while ((ze = zis.getNextEntry()) != null) {
                if (!ze.isDirectory()) {
                    final byte[] contents = ZIP.readEntry(zis)
                    final String key = ze.getName().replace("\\", "/")
                    entries.put(key, new String(contents))
                }
            }
        }
        return entries
    }
}
