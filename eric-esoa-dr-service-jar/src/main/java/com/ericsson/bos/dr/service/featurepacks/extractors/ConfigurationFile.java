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

import java.util.function.Supplier;

import com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType;

/**
 * A configuration file in the feature pack archive.
 */
public class ConfigurationFile {

    private String filename;
    private FeaturePackFileType type;
    private byte[] originalContents;
    private Supplier<?> configSupplier;

    /**
     * ConfigurationFile.
     * @param filename file name
     * @param type file type
     * @param contents contents
     * @param configSupplier supplier to convert contents to config object
     */
    public ConfigurationFile(String filename, FeaturePackFileType type, byte[] contents, final Supplier<?> configSupplier) {
        this.filename = filename;
        this.type = type;
        this.originalContents = contents;
        this.configSupplier = configSupplier;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getOriginalContents() {
        return originalContents;
    }

    /**
     * Get configuration object supplier.
     * @param <T> type of configuration object, e.g ApplicationConfigurationDto
     * @return supplier
     */
    public <T> Supplier<T> getConfigSupplier() {
        return (Supplier<T>) configSupplier;
    }

    public FeaturePackFileType getType() {
        return type;
    }
}