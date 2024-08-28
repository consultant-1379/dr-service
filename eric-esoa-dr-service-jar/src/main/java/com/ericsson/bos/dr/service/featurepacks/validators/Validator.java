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
package com.ericsson.bos.dr.service.featurepacks.validators;

import java.util.List;

import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;

/**
 * Feature pack validator.
 */
public interface Validator {

    /**
     * Perform validation of the files in the feature pack.
     * @param featureName feature pack name
     * @param featurePackFiles files in the feature pack
     */
    void validate(String featureName, List<ConfigurationFile> featurePackFiles);
}
