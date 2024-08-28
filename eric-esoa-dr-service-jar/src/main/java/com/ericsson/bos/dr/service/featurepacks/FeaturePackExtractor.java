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
package com.ericsson.bos.dr.service.featurepacks;

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_IO_READ_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFileExtractor;
import com.ericsson.bos.dr.service.utils.ZIP;

/**
 * FeaturePack extractor.
 */
@Component
public class FeaturePackExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturePackExtractor.class);

    @Autowired
    private List<ConfigurationFileExtractor> extractors;

    /**
     * Extract all files from the feature pack archive.
     *
     * @param file multi-part file.
     * @return <code>ConfigurationFile</code> list
     */
    public List<ConfigurationFile> unpack(final MultipartFile file) {
        try {
            final List<ConfigurationFile> files = new ArrayList<>();
            try (var zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        final byte[] contents = ZIP.readEntry(zis);
                        final String type = new File(ze.getName()).getParent();
                        if (type != null) {
                            final var configurationFile =
                                    ConfigurationFileExtractor.getInstanceByType(type, extractors).extract(ze, contents);
                            files.add(configurationFile);
                        }
                    }
                }
            }
            return files;
        } catch (final IOException e) {
            LOGGER.error("Error processing archive: " + file.getOriginalFilename(), e);
            throw new DRServiceException(FP_IO_READ_ERROR, file.getOriginalFilename());
        }
    }
}