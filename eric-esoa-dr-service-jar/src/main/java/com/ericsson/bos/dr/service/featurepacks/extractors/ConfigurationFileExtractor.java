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
package com.ericsson.bos.dr.service.featurepacks.extractors;

import java.util.List;
import java.util.zip.ZipEntry;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;

/**
 * Extract a configuration file from the feature pack archive.
 */
public interface ConfigurationFileExtractor {

    /**
     * Extract zip entry to <code>ConfigurationFile</code>.
     * @param zipEntry zip entry.
     * @param contents zip file contents.
     * @return <code>ConfigurationFile</code>
     */
    ConfigurationFile extract(ZipEntry zipEntry, byte[] contents);

    /**
     * Check if extractor supports the configuration file type.
     * @param fileType file type
     * @return true if supported, otherwise false
     */
    boolean supports(String fileType);

    /**
     * Get <code>ConfigurationFileExtractor</code> instance to handle the specified file type.
     * @param type file type
     * @param extractors list of all <code>ConfigurationFileExtractor</code> implementations
     * @return ConfigurationFileExtractor
     */
    static ConfigurationFileExtractor getInstanceByType(final String type,
                                                        final List<ConfigurationFileExtractor> extractors) {
        return extractors.stream().filter(fp -> fp.supports(type)).findFirst()
                .orElseThrow(() -> new DRServiceException(ErrorCode.FP_FOLDER_ERROR, type));
    }
}