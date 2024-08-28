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

package com.ericsson.bos.dr.service.listeners;

import java.util.Map;

import com.ericsson.bos.dr.service.utils.JQ;
import com.ericsson.bos.dr.service.utils.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Evaluate JQ expression in listener configuration.
 */
@Component
public class JqExpression implements ListenerExpression {

    @Override
    public String evaluate(String expression, Map<String, Object> context, long featurePackId) {
        final var expressionResult = JQ.query(expression, JSON.convert(context, JsonNode.class)).getObject();
        if (expressionResult instanceof Number || expressionResult instanceof String) {
            return String.valueOf(expressionResult);
        } else {
            return JSON.toString(expressionResult);
        }
    }

    @Override
    public boolean supports(String expression) {
        return expression.startsWith("jq");
    }
}