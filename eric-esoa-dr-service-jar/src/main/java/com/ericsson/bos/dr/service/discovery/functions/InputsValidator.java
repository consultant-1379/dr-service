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
package com.ericsson.bos.dr.service.discovery.functions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationInputsDto;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * Validate mandatory inputs are provided.
 */
@Component
public class InputsValidator  {

    /**
     * Validate mandatory inputs are provided.
     * @param jobInputs discover or reconcile inputs
     * @param userInputs the provided user inputs
     */
    public void validate(List<ApplicationConfigurationInputsDto> jobInputs, final Map<String, Object> userInputs) {
        final List<String> missingInputs = jobInputs.stream()
                .filter(ApplicationConfigurationInputsDto::getMandatory)
                .map(ApplicationConfigurationInputsDto::getName)
                .filter(n -> !userInputs.containsKey(n))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(missingInputs)) {
            throw new DRServiceException(ErrorCode.MISSING_INPUTS, missingInputs.toString());
        }
    }
}