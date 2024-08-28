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

/**
 * Listener expression.
 */
public interface ListenerExpression {

    /**
     * Evaluate the expression.
     * @param expression expression
     * @param context context to which the expression is applied
     * @param featurePackId feature pack id
     * @return evaluated value
     */
    String evaluate(String expression, Map<String, Object> context, long featurePackId);

    /**
     * Return true if implementation can evaluate the expression.
     * @param expression expression
     * @return true if expression can be evaluated
     */
    boolean supports(String expression);
}