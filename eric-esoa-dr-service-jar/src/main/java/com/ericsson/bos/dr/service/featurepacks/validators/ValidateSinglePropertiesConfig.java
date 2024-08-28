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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.PROPERTIES;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;

/**
 * Validate the feature pack contains a single Properties configuration file.
 */
@Component
@Order(5)
public class ValidateSinglePropertiesConfig implements Validator {

    @Override
    public void validate(final String featurePackName, List<ConfigurationFile> featurePackFiles) {
        if (featurePackFiles.stream().filter(cf -> PROPERTIES.equals(cf.getType())).count() > 1) {
            throw new DRServiceException(ErrorCode.MULTIPLE_PROPERTIES_CONFIG, featurePackName);
        }
    }
}