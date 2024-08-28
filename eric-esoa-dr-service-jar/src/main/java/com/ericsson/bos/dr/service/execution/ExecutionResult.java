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

import com.ericsson.bos.dr.service.execution.executors.CommandResponse;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * An Execution Result from the <code>ExecutionEngine</code>.
 */
public class ExecutionResult {

    private CommandResponse commandResponse;

    private List<Map<String, Object>> mappedCommandResponse;

    /**
     * ExecutionResult.
     * @param commandResponse the command response.
     * @param mappedCommandResponse the mapped command response as per the defined jq mappings.
     */
    public ExecutionResult(CommandResponse commandResponse, List<Map<String, Object>> mappedCommandResponse) {
        this.commandResponse = commandResponse;
        this.mappedCommandResponse = mappedCommandResponse;
    }

    public String getCommand() {
        return  commandResponse.getCommand();
    }

    public String getCommandResponse() {
        return commandResponse.getResponse();
    }

    public List<Map<String, Object>> getMappedCommandResponse() {
        return mappedCommandResponse;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}