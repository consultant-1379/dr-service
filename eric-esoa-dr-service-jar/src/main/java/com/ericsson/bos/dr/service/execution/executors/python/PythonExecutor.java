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
package com.ericsson.bos.dr.service.execution.executors.python;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.executors.CommandExecutor;
import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import com.ericsson.bos.dr.service.execution.executors.command.ProcessExecutor;

/**
 * Constructs and executes python scripts
 */
@Component
public class PythonExecutor implements CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonExecutor.class);

    @Autowired
    private PythonCommandProcessor pythonCommandProcessor;

    @Autowired
    private ProcessExecutor processExecutor;

    @Autowired
    private PythonEnvSupplier pythonEnvSupplier;

    @Override
    public CommandResponse execute(final ExecutionContext executionContext) {
        final var pythonCommand = pythonCommandProcessor.parse(executionContext);
        final String command = pythonCommand.getFinalCommand();
        final long startTime = System.currentTimeMillis();
        final CommandResponse commandResponse;
        try {
            final Map<String, String> env = pythonEnvSupplier.get();
            commandResponse = processExecutor.executeProcess(command, env);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandExecutorException(pythonCommand.getFinalCommand(), e);
        } catch (final CommandExecutorException e) {
            throw e;
        } catch (final Exception e) {
            throw new CommandExecutorException(pythonCommand.getFinalCommand(), e);
        }
        final long endTime = System.currentTimeMillis();
        LOGGER.info("Command :: {} execute. Command took :: {} ", command, (endTime - startTime));
        return commandResponse;
    }

    @Override
    public boolean canExecute(final String type) {
        return "python".equalsIgnoreCase(type);
    }
}
