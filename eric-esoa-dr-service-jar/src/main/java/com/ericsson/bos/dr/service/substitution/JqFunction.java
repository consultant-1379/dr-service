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

import com.ericsson.bos.dr.service.utils.JQ;
import com.ericsson.bos.dr.service.utils.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Built-in jinja function for JqFunction.
 */
public class JqFunction extends ELFunctionDefinition {

    private static final Logger LOGGER = LoggerFactory.getLogger(JqFunction.class);
    /**
     * Constructor
     */
    public JqFunction() {
        super( "fn", "jq", JqFunction.class, "jq", String.class, String.class);
    }

    /**
     * Execute jq expression on a json string.
     * @param jqExpressions jqExpressions
     * @param json json
     * @return substituted jq result
     */
    public static String jq(String json, String jqExpressions) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.info("jq context: {}", json);
            LOGGER.info("jq expression: {}", jqExpressions);
        }
        final var result =  JQ.query(jqExpressions, JSON.read(json, JsonNode.class)).getObject();
        if (result instanceof String) {
            return result.toString();
        } else {
            return JSON.toString(result);
        }
    }
}