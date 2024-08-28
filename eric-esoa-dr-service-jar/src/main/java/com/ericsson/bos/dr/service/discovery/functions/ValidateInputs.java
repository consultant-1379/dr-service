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
import java.util.function.Consumer;

import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationInputsDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validate the supplied discovery inputs.
 * If autoReconcile is enabled, then reconcile inputs are also validated.
 */
@Component
@DiscoveryFunction
public class ValidateInputs implements Consumer<DiscoveryContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateInputs.class);

    @Autowired
    private InputsValidator inputsValidator;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        LOGGER.info("Validating inputs for jobId={}", discoveryContext.getJobId());
        final List<ApplicationConfigurationInputsDto>  discoveryInputs =
                discoveryContext.getDiscoveryJobConf().getInputs();
        if (discoveryInputs != null) {
            inputsValidator.validate(discoveryInputs, discoveryContext.getInputs());
        }

        if (discoveryContext.isAutoReconcile()) {
            final List<ApplicationConfigurationInputsDto> reconcileInputs =
                    discoveryContext.getJobConf().getReconcile().getInputs();
            if (reconcileInputs != null) {
                inputsValidator.validate(reconcileInputs, discoveryContext.getInputs());
            }
        }
    }
}