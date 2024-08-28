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

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds information related to the python command
 */
@Getter
@Setter
public class PythonCommand {
    private static final String TYPE = "python3";
    private String command;
    private List<Object> substitutedProperties = new ArrayList<>();
    private Path path;

    /**
     * Builds the final command
     * Includes prefix with python, the path and then any arguments
     *
     * @return final command
     */
    public String getFinalCommand() {
        final var stringBuilder = new StringBuilder(TYPE);
        stringBuilder.append(" ").append(this.path);
        this.substitutedProperties
                .forEach(prop -> stringBuilder.append(" ").append(prop));
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}