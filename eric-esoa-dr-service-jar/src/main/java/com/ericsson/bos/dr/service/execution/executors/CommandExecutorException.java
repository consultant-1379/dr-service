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

package com.ericsson.bos.dr.service.execution.executors;

/**
 * Command Executor Exception.
 */
public class CommandExecutorException extends RuntimeException {

    private final String command;
    private final String commandOutput;

    /**
     * Command execution failed with output.
     * @param command executed command
     * @param commandOutput the output of the executed command
     */
    public CommandExecutorException(final String command, final String commandOutput) {
        super(String.format("Command '%s' failed with output '%s'", command, commandOutput));
        this.command = command;
        this.commandOutput = commandOutput;
    }

    /**
     * Command execution failed without output.
     * @param command the command to be executed or which has been executed
     * @param message additional details. of the failure
     * @param cause the cause of the command failure
     */
    public CommandExecutorException(final String command, final String message, final Throwable cause) {
        super(message, cause);
        this.command = command;
        this.commandOutput = null;
    }

    /**
     * Command execution failed without output.
     * @param command the command to be executed or which has been executed
     * @param cause the cause of the command failure
     */
    public CommandExecutorException(final String command, final Throwable cause) {
        super(cause);
        this.command = command;
        this.commandOutput = null;
    }

    public String getCommand() {
        return command;
    }

    public String getCommandOutput() {
        return commandOutput;
    }
}