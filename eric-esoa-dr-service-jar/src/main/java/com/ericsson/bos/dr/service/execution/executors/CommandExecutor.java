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

import com.ericsson.bos.dr.service.execution.ExecutionContext;

/**
 * Command executor, for example a REST or SHELL executor.
 */
public interface CommandExecutor {

    /**
     * Execute the command.
     * @param executionContext execution context
     * @return <code>CommandResponse</code>
     */
    CommandResponse execute(ExecutionContext executionContext);

    /**
     * Check if executor support the type defined in the action.
     * @param type the command type e.g rest, shell
     * @return true if executor supports the command type
     */
    boolean canExecute(String type);
}