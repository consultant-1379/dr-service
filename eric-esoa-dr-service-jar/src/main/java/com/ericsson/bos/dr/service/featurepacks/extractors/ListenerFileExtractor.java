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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.LISTENER;
import java.io.File;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

import com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType;
import com.ericsson.bos.dr.service.utils.YAML;
import com.ericsson.bos.dr.web.v1.api.model.ListenerConfigurationDto;
import org.springframework.stereotype.Component;

/**
 * Extract a Listener configuration file from the feature pack archive.
 */
@Component
public class ListenerFileExtractor implements ConfigurationFileExtractor {

    @Override
    public ConfigurationFile extract(ZipEntry zipEntry, byte[] contents) {
        final String filename = new File(zipEntry.getName()).getName();
        final Supplier<ListenerConfigurationDto> configSupplier =
                () -> YAML.read(contents, ListenerConfigurationDto.class);
        return new ConfigurationFile(filename, FeaturePackFileType.LISTENER, contents, configSupplier);
    }

    @Override
    public boolean supports(String folder) {
        return LISTENER.getFolder().equalsIgnoreCase(folder);
    }
}