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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Evaluate expressions in listener configuration.
 */
@Component
public class ListenerExpressionEvaluator {

    private static final String EXRP_CONTENT_REGEX = "(?<=\\().*(?=\\))";

    @Autowired
    private List<ListenerExpression> listenerExpressions;

    /**
     * Evaluate supported Jinja and JQ expression in listener configuration.
     * Returns the original expression if it is neither a jinja nor jq expression.
     * @param expression expression
     * @param evaluationContext context to which the expression is applied
     * @param featurePackId feature pack id
     * @return evaluate expression value
     */
    public String evaluate(final String expression, final Map<String, Object> evaluationContext, final long featurePackId) {
        return listenerExpressions.stream()
                .filter(l -> l.supports(expression))
                .findAny()
                .map(l -> l.evaluate(getExpressionContent(expression), evaluationContext, featurePackId))
                .orElse(expression);
    }

    private String getExpressionContent(String expression) {
        final var matcher = Pattern.compile(EXRP_CONTENT_REGEX).matcher(expression);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        return matcher.group();
    }
}