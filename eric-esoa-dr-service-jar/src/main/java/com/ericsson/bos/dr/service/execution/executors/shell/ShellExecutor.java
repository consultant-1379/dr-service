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
import com.ericsson.bos.dr.service.execution.executors.CommandExecutor;
import com.ericsson.bos.dr.service.execution.executors.CommandExecutorException;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import com.ericsson.bos.dr.service.execution.executors.command.ProcessExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Execute shell command.
 */
@Component
public class ShellExecutor implements CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellExecutor.class);

    @Autowired
    private ProcessExecutor processExecutor;

    @Autowired
    private ShellCommandProcessor shellCommandProcessor;

    @Override
    public CommandResponse execute(ExecutionContext executionContext) {
        final String command = shellCommandProcessor.parse(executionContext);
        try {
            LOGGER.info("Command :: {} execute. ", command);
            return processExecutor.executeProcess(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandExecutorException(command, e);
        } catch (CommandExecutorException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandExecutorException(command, e);
        }
    }

    @Override
    public boolean canExecute(String type) {
        return "shell".equalsIgnoreCase(type);
    }
}