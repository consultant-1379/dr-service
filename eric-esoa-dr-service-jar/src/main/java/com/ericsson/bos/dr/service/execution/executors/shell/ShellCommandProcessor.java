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

package com.ericsson.bos.dr.service.execution.executors.shell;

import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Performs substitution on shell command.
 */
@Component
public class ShellCommandProcessor {

    @Autowired
    private SubstitutionEngine substitutionEngine;

    /**
     * Parse the command to substitute jinja expressions.
     * @param executionContext execution context
     * @return parsed command string
     */
    public String parse(ExecutionContext executionContext) {
        final String shellCommand = executionContext.getActionDto().getCommand().trim();
        return substitutionEngine.render(shellCommand, executionContext.getSubstitutionCtx(), executionContext.getFeaturePackId());
    }
}
