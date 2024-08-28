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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.ASSET;
import java.io.File;
import java.util.zip.ZipEntry;

import org.springframework.stereotype.Component;

/**
 * Extract an Asset file from the feature pack archive.
 */
@Component
public class AssetFileExtractor implements ConfigurationFileExtractor {

    @Override
    public ConfigurationFile extract(ZipEntry zipEntry, byte[] contents) {
        final String filename = new File(zipEntry.getName()).getName();
        return new ConfigurationFile(filename, ASSET, contents, null);
    }

    @Override
    public boolean supports(String folder) {
        return ASSET.getFolder().equalsIgnoreCase(folder);
    }
}
