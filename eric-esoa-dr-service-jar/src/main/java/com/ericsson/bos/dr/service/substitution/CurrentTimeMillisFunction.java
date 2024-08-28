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

/**
 * Built-in jinja function for CurrentTimeStampFunction.
 */
public class CurrentTimeMillisFunction extends ELFunctionDefinition {

    /**
     * Constructor
     */
    public CurrentTimeMillisFunction() {
        super("fn", "currentTimeMillis", CurrentTimeMillisFunction.class, "currentTimeMillis", null);
    }

    /**
     * Implementation of currentTimeMillis function
     *
     * @return system currentTimeMillis
     */
    public static Long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
