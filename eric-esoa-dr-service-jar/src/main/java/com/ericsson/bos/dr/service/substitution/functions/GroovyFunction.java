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

package com.ericsson.bos.dr.service.substitution.functions;

import static com.ericsson.bos.dr.service.substitution.SubstitutionEngine.FP_CTX_VAR;
import java.util.Optional;

import com.ericsson.bos.dr.service.AssetService;
import com.ericsson.bos.dr.service.utils.Groovy;
import com.ericsson.bos.dr.service.utils.SpringContextHolder;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateError;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import groovy.lang.Script;

/**
 * Built-in jinja function to support execute of groovy expressions or scripts.
 */
public class GroovyFunction extends ELFunctionDefinition {

    /**
     * GroovyFunction.
     */
    public GroovyFunction() {
        super("fn", "groovy", GroovyFunction.class, "eval", String.class, Object[].class);
    }

    /**
     * Evaluate the groovy expression. An expression prefixed with '@' will be treated as
     * a reference to an asset which will be fetched and executed.
     * @param expression  groovy expression
     * @param args arguments
     * @return evaluated value
     */
    public static Object eval(String expression, Object... args) {
        try {
            final Object result;
            if (expression.startsWith("@")) {
                result = Groovy.evalScript(getGroovyScript(expression), args);
            } else {
                result = Groovy.evalExpression(expression, args);
            }
            return result;
        } catch (final Exception e) {
            JinjavaInterpreter.getCurrent().addError(TemplateError.fromException(e));
        }
        return null;
    }

    private static Class<Script> getGroovyScript(String expression) {
        final var scriptName = expression.substring(1);
        final var assetService = SpringContextHolder.getBean(AssetService.class);
        final var featurePackId = (Long) Optional.ofNullable(JinjavaInterpreter.getCurrent().getContext().get(FP_CTX_VAR))
                .orElseThrow(() -> new IllegalStateException("Feature Pack id is not set in the context"));
        return assetService.getGroovyScript(scriptName, featurePackId);
    }
}