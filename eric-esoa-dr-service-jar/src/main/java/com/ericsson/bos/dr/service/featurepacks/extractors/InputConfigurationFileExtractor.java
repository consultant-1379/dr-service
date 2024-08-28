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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.INPUT;

import java.io.File;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType;
import com.ericsson.bos.dr.service.utils.YAML;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;

/**
 * Extract an Input configuration file from the feature pack archive.
 */
@Component
public class InputConfigurationFileExtractor implements ConfigurationFileExtractor {

    @Override
    public ConfigurationFile extract(ZipEntry zipEntry, byte[] contents) {
        final String filename = new File(zipEntry.getName()).getName();
        final Supplier<InputConfigurationDto> configSupplier =
                () -> YAML.read(contents, InputConfigurationDto.class);
        return new ConfigurationFile(filename, FeaturePackFileType.INPUT, contents, configSupplier);
    }

    @Override
    public boolean supports(String folder) {
        return INPUT.getFolder().equalsIgnoreCase(folder);
    }
}