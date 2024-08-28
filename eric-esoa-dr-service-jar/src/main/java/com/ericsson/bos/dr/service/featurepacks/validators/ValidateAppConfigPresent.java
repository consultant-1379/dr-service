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

import static com.ericsson.bos.dr.service.featurepacks.FeaturePackFileType.APPLICATION;
import java.util.List;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.featurepacks.extractors.ConfigurationFile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validate the feature pack contain an Application configuration file.
 */
@Component
@Order(2)
public class ValidateAppConfigPresent implements Validator {

    @Override
    public void validate(final String featurePackName, List<ConfigurationFile> featurePackFiles) {
        if (featurePackFiles.stream().noneMatch(cf -> APPLICATION.equals(cf.getType()))) {
            throw new DRServiceException(ErrorCode.MISSING_APP_CONFIG, featurePackName);
        }
    }
}