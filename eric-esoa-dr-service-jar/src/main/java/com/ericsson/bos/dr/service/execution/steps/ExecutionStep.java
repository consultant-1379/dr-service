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
package com.ericsson.bos.dr.service.execution.steps;

import com.ericsson.bos.dr.service.execution.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An execution step.
 * @param <I> The type of input required by a step.
 * @param <O> The type of output produced by a step.
 */
public interface ExecutionStep<I,O> {

    Logger LOGGER = LoggerFactory.getLogger(ExecutionStep.class);

    /**
     * Execute the step.
     * @param input the step input.
     * @param executionContext the action execution context.
     * @return step output
     */
    O execute(I input, ExecutionContext executionContext);
}
