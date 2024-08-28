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
import org.apache.commons.lang3.StringUtils;

/**
 * Built-in jinja function for ReplaceAtSymbolFunction.
 */
public class ReplaceAtSymbolFunction extends ELFunctionDefinition {

    /**
     * Constructor
     */
    public ReplaceAtSymbolFunction() {
        super("fn", "replaceAtSymbol", ReplaceAtSymbolFunction.class, "replaceAtSymbol", String.class);
    }

    /**
     * Implementation of replaceAtSymbol function
     * @param originalStr originalStr
     * @return substituted string
     */
    public static String replaceAtSymbol(String originalStr) {
        return StringUtils.replace(originalStr, "@", "__");
    }
}
