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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.PROPERTIES;

import java.io.File;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.service.utils.YAML;
import com.ericsson.bos.dr.web.v1.api.model.PropertiesConfigurationDto;

/**
 * Extract a Properties configuration file from the feature pack archive.
 */
@Component
public class PropertiesFileExtractor implements ConfigurationFileExtractor {

    @Override
    public ConfigurationFile extract(ZipEntry zipEntry, byte[] contents) {
        final String filename = new File(zipEntry.getName()).getName();
        final Supplier<PropertiesConfigurationDto> configSupplier =
                () -> YAML.read(contents, PropertiesConfigurationDto.class);
        return new ConfigurationFile(filename, PROPERTIES, contents, configSupplier);
    }

    @Override
    public boolean supports(String folder) {
        return PROPERTIES.getFolder().equalsIgnoreCase(folder);
    }
}