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

import org.springframework.core.io.Resource;

/**
 * Holds the feature pack Resource and archive name.
 */
public class FeaturePackDownloadResource {
    private String featurePackArchiveName;
    private Resource resource;

    /**
     * Constructor.
     *
     * @param featurePackName
     *         feature pack name
     * @param resource
     *         feature pack resource
     */
    public FeaturePackDownloadResource(final String featurePackName, final Resource resource) {
        this.featurePackArchiveName = featurePackName + ".zip";
        this.resource = resource;
    }

    /**
     * Return feature pack name
     *
     * @return feature pack name
     */
    public String getFeaturePackArchiveName() {
        return featurePackArchiveName;
    }

    /**
     * Return feature pack resource.
     *
     * @return feature pack resource
     */
    public Resource getResource() {
        return resource;
    }
}
