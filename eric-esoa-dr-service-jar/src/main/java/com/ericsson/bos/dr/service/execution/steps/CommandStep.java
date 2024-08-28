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

import java.util.List;
import java.util.Map;

import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.executors.CommandExecutor;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Command step which executes the defined command and returns the command response string.
 */
@Component
public class CommandStep implements ExecutionStep<Map<String, Object>, CommandResponse>{

    @Autowired
    private List<CommandExecutor> executors;

    @Override
    public CommandResponse execute(Map<String, Object> substitutionCtx, ExecutionContext executionContext) {
        final var commandType = executionContext.getActionDto().getType().toString();
        final CommandExecutor executor = executors.stream().filter(e -> e.canExecute(commandType)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("No executor found for type " + commandType));
        final var cmdExecutionContext = new ExecutionContext(executionContext.getFeaturePackId(), executionContext.getActionDto(), substitutionCtx);
        final var commandResponse = executor.execute(cmdExecutionContext);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("Command output: {}", commandResponse);
        }
        return commandResponse;
    }
}