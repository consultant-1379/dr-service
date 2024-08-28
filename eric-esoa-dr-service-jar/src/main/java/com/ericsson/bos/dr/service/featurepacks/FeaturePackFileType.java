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

/**
 * File type in a feature pack.
 */
public enum FeaturePackFileType {

    APPLICATION("applications", "appconfig_schema.json"),
    PROPERTIES("properties", "properties_schema.json"),
    INPUT("job_inputs", "input_schema.json"),
    ASSET("assets", null),
    LISTENER("listeners", "listener_schema.json");

    private final String folder;
    private final String schema;

    /**
     * FeaturePackFileType.
     * @param folder containing folder in feature pack archive
     * @param schema the associated schema file
     */
    FeaturePackFileType(final String folder, final String schema) {
        this.folder = folder;
        this.schema = schema;
    }

    public String getFolder() {
        return folder;
    }

    public String getSchema() {
        return "/schemas/".concat(schema);
    }

    /**
     * Check if there is a schema associated with the file type.
     * @return true if there is a schema for the file type
     */
    public boolean hasSchema() {
        return schema != null;
    }
}