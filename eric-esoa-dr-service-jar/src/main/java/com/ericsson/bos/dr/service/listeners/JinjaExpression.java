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

import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Evaluate jinja expression in listener configuration.
 */
@Component
public class JinjaExpression implements ListenerExpression {

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Override
    public String evaluate(String expression, final Map<String, Object> context, final long featurePackId) {
        return substitutionEngine.render(expression, context, featurePackId);
    }

    @Override
    public boolean supports(String expression) {
        return expression.startsWith("jinja");
    }
}
