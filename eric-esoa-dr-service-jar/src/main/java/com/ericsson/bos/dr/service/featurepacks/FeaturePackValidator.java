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

import java.util.List;

import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;
import com.ericsson.bos.dr.service.featurepacks.validators.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FeaturePack validator.
 */
@Component
public class FeaturePackValidator {

    @Autowired
    private List<Validator> validators;

    /**
     * Validates the contents of a feature pack archive.
     * @param featurePackName feature pack name
     * @param featurePackFiles all files in the feature pack
     */
    public void validate(final String featurePackName, final List<ConfigurationFile> featurePackFiles) {
        validators.forEach(v -> v.validate(featurePackName, featurePackFiles));
    }
}