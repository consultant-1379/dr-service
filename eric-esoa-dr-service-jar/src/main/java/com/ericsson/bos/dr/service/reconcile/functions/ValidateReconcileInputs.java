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

package com.ericsson.bos.dr.service.reconcile.functions;

import java.util.function.Consumer;

import com.ericsson.bos.dr.service.discovery.functions.InputsValidator;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationReconcileDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Validate the supplied reconcile inputs.
 */
@Component
@ReconcileFunction
public class ValidateReconcileInputs implements Consumer<ReconcileContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateReconcileInputs.class);

    @Autowired
    private InputsValidator inputsValidator;

    @Override
    public void accept(ReconcileContext reconcileContext) {
        final ApplicationConfigurationReconcileDto reconcileJobConf = reconcileContext.getJobConf().getReconcile();
        if (reconcileJobConf.getInputs() == null) {
            return;
        }
        LOGGER.info("Validating reconcile inputs for jobId={}", reconcileContext.getJobId());
        inputsValidator.validate(reconcileJobConf.getInputs(), reconcileContext.getInputs());
    }
}