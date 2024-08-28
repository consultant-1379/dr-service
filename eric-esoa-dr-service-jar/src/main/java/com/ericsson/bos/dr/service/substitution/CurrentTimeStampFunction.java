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
package com.ericsson.bos.dr.service.substitution;

import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Built-in jinja function for CurrentTimeStampFunction.
 */
public class CurrentTimeStampFunction extends ELFunctionDefinition {

    /**
     * Constructor
     */
    public CurrentTimeStampFunction() {
        super("fn", "currentTimeStamp", CurrentTimeStampFunction.class, "currentTimeStamp", String.class);
    }

    /**
     * Implementation of currentTimeStamp function
     * @param originalStr originalStr
     * @return substituted currentTimeStamp
     *
     */
    public static String currentTimeStamp(String originalStr) {
        return DateTimeFormatter.ofPattern(originalStr).format(ZonedDateTime.now());
    }
}
