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
package com.ericsson.bos.dr.service.execution;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import com.ericsson.bos.dr.service.execution.steps.CommandStep;
import com.ericsson.bos.dr.service.execution.steps.ExecutionStep;
import com.ericsson.bos.dr.service.execution.steps.JsonMappingStep;
import com.ericsson.bos.dr.service.execution.steps.PostFunctionStep;
import com.ericsson.bos.dr.service.execution.steps.PreFunctionStep;
import com.ericsson.bos.dr.service.utils.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Execution Engine is capable of executing an action defined by <code>ApplicationConfigurationActionDto</code>.
 * The engine performs the following steps as defined in the action.
 * <ol>
 *     <li>Optionally execute PreFunction. The output of the PreFunction is available for
 *     substitution in the command step via 'PreFunction' key.</li>
 *     <li>Execute the command defined in the action. The appropriate <code>CommandExecutor</code>
 *     is identified by the type in the action.</li>
 *     <li>Optionally execute the PostFunction. The postFunction can perform jinja substitution on
 *     the command output string. For example executing a jinja function to remove unwanted characters
 *     in the response string.</li>
 *     <li>Map the command response using JQ, as per the mappings defined in the action.</li>
 * </ol>
 */
@Component
public class ExecutionEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionEngine.class);

    @Autowired
    private List<ExecutionStep<?, ?>> executionSteps;

    /**
     * Execute the action with available substitution context.
     * @param executionContext the action execution context
     * @return <code>ExecutionResult</code>
     */
    public ExecutionResult execute(ExecutionContext executionContext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing action with ctx: {}", executionContext);
        }

        final Map<String, Object> initialInput = executionContext.getSubstitutionCtx();
        final Map<String, Object> preFunctionOutput = executeStep(getStep(PreFunctionStep.class), initialInput, executionContext, exceptionHandler());
        final CommandResponse commandOutput = executeStep(getStep(CommandStep.class), preFunctionOutput, executionContext, exceptionHandler());
        final String postFunctionOutput = executeStep(getStep(PostFunctionStep.class), commandOutput.getResponse(), executionContext,
                commandOutputExceptionHandler(commandOutput));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("PreFunction: {}, CommandResponse: {}, PostFunction: {}",
                    JSON.toString(preFunctionOutput), commandOutput, postFunctionOutput);
        }
        final List<Map<String, Object>> jsonMappingOutput = executeStep(getStep(JsonMappingStep.class), postFunctionOutput, executionContext,
                commandOutputExceptionHandler(commandOutput));
        final var executionResult = new ExecutionResult(commandOutput, jsonMappingOutput);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Execution Result: {}", executionResult);
        }
        return executionResult;
    }

    private <I, O> O executeStep(ExecutionStep<I, O> executionStep, I input,
                                 ExecutionContext executionContext,
                                 BiFunction<ExecutionStep<I,O>, Exception, ExecutionEngineException>  exceptionHandler) {
        try {
            return executionStep.execute(input, executionContext);
        } catch (final CommandExecutorException e) {
            throw new ExecutionEngineException(e, executionStep.getClass().getSimpleName(),
                    new CommandResponse(e.getCommand(), e.getCommandOutput()));
        } catch (final Exception e) {
            throw exceptionHandler.apply(executionStep, e);
        }
    }

    private <I,O> BiFunction<ExecutionStep<I,O>, Exception, ExecutionEngineException> exceptionHandler() {
        return (step, e) -> new ExecutionEngineException(e, step.getClass().getSimpleName());
    }

    private <I,O> BiFunction<ExecutionStep<I,O>, Exception, ExecutionEngineException> commandOutputExceptionHandler(CommandResponse commandResponse) {
        return  (step, e) ->  new ExecutionEngineException(e, step.getClass().getSimpleName(), commandResponse);
    }

    private <I, O> ExecutionStep<I, O> getStep(final Class<? extends ExecutionStep> clazz) {
        return (ExecutionStep<I, O>) executionSteps.stream().filter(step -> step.getClass().equals(clazz)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Invalid step " + clazz.getName()));
    }
}