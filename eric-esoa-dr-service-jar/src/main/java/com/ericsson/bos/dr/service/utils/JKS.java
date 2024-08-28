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

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;

/**
 * Creates JKS files on the file system.
 */
public class JKS {

    private static final Logger LOGGER = LoggerFactory.getLogger(JKS.class);

    /**
     * Write a JKS file to the file system.
     *
     * @param storeData
     *         certificate data to store
     * @param storePassword
     *         JKS store pwd
     * @return Path on the file system to the JKS sile.
     */
    public static void write(final byte[] storeData, final String storePassword, final Path storePath) {

        LOGGER.debug("Writing JKS: {}", storePath);
        try {
            Files.createDirectories(storePath.getParent());
            try (FileOutputStream fos = new FileOutputStream(storePath.toFile());
                    ByteArrayInputStream bais = new ByteArrayInputStream(storeData)) {
                final KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
                clientKeyStore.load(bais, StringUtils.hasText(storePassword) ? storePassword.toCharArray() : null);
                clientKeyStore.store(fos, StringUtils.hasText(storePassword) ? storePassword.toCharArray() : null);
            }
        } catch (Exception e) {
            throw new DRServiceException(ErrorCode.JKS_FILE_ERROR, e.getMessage());
        }
    }
}
