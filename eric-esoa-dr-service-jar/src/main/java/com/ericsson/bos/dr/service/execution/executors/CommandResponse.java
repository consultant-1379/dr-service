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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.bos.dr.service.utils.JSON;

/**
 * Command response.
 */
public class CommandResponse {

    private String command;
    private String response;

    /**
     * CommandResponse.
     * @param command the executed command
     * @param commandResponse the command response
     */
    public CommandResponse(String command, String commandResponse) {
        this.command = command;
        // We compact the response for more efficient storage in the db
        // and to make the logs more readable.
        this.response = JSON.compact(commandResponse);
    }

    public String getCommand() {
        return command;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}