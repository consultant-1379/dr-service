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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.APPLICATION;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.ASSET;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.INPUT;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.LISTENER;
import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.PROPERTIES;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ericsson.bos.dr.jpa.model.ConfigurationFileEntity;
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.utils.YAML;
import com.ericsson.bos.dr.service.utils.ZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

/**
 * FeaturePack archiver.
 */
@Component
public class FeaturePackArchiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturePackArchiver.class);
    private static final String SEPARATOR = "/";
    /**
     * Create zip archive containing all files in the feature pack.
     *
     * @param featurePackEntity feature pack entity
     * @return ByteArrayResource
     */
    public ByteArrayResource zip(final FeaturePackEntity featurePackEntity) {
        final var bos = new ByteArrayOutputStream();
        try {
            final var zos = new ZipOutputStream(bos);
            zos.putNextEntry(new ZipEntry(APPLICATION.getFolder() + SEPARATOR));
            zos.putNextEntry(new ZipEntry(PROPERTIES.getFolder() + SEPARATOR));
            zos.putNextEntry(new ZipEntry(INPUT.getFolder() + SEPARATOR));
            zos.putNextEntry(new ZipEntry(ASSET.getFolder() + SEPARATOR));
            zos.putNextEntry(new ZipEntry(LISTENER.getFolder() + SEPARATOR));

            featurePackEntity.getApplications().stream().forEach(app -> writeFile(APPLICATION, app, zos));
            featurePackEntity.getProperties().ifPresent(properties -> writeFile(PROPERTIES, properties, zos));
            featurePackEntity.getInputs().stream().forEach(inputs -> writeFile(INPUT, inputs, zos));
            featurePackEntity.getListeners().stream().forEach(listener -> writeFile(LISTENER, listener, zos));
            featurePackEntity.getAssets().stream().forEach(asset -> writeFile(ASSET, asset, zos));

            zos.close();
        } catch (final IOException e) {
            LOGGER.error("I/O error processing feature pack archive: " + featurePackEntity.getName(), e);
            throw new DRServiceException(ErrorCode.FP_IO_WRITE_ERROR, featurePackEntity.getName());
        }
        return new ByteArrayResource(bos.toByteArray());
    }

    private void writeFile(final FeaturePackFileType fileType, final ConfigurationFileEntity<?> configEntity, final ZipOutputStream zos) {
        try {
            final String filepath = fileType.getFolder() + File.separator + configEntity.getFilename();
            final Object data = configEntity.getConfig();
            if (data instanceof byte[]) {
                ZIP.writeEntry(filepath, (byte[]) data, zos);
            } else {
                final byte[] contents = Optional.ofNullable(configEntity.getContents())
                        .orElseGet(() -> YAML.write(configEntity.getConfig()));
                ZIP.writeEntry(filepath, contents, zos);
            }
        } catch (IOException e) {
            LOGGER.error("Error writing config file to archive: " + configEntity.getName(), e);
            throw new DRServiceException(ErrorCode.FP_IO_WRITE_ERROR, configEntity.getFeaturePack().getName());
        }
    }
}