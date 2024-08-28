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

import java.util.Optional;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.execution.executors.CommandResponse;

/**
 * Execution Engine Exception.
 */
public class ExecutionEngineException extends DRServiceException {

    private final String command;
    private final String commandOutput;

    /**
     * ExecutionEngineException
     * @param cause cause
     * @param step the originating execution step
     */
    public ExecutionEngineException(Throwable cause, String step) {
        super(cause, ErrorCode.EXECUTION_STEP_ERROR, step, cause.getMessage());
        this.command = null;
        this.commandOutput = null;
    }

    /**
     * ExecutionEngineException
     * @param cause cause
     * @param step the originating execution step
     * @param commandResponse the command response
     */
    public ExecutionEngineException(Throwable cause, String step, final CommandResponse commandResponse) {
        super(cause, ErrorCode.EXECUTION_STEP_ERROR, step, cause.getMessage());
        this.command = commandResponse.getCommand();
        this.commandOutput = commandResponse.getResponse();
    }

    public Optional<String> getCommand() {
        return Optional.ofNullable(command);
    }

    public Optional<String> getCommandOutput() {
        return Optional.ofNullable(commandOutput);
    }
}