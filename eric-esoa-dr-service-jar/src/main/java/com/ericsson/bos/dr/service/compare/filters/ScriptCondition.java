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

package com.ericsson.bos.dr.service.compare.filters;

import java.util.Map;

import com.ericsson.bos.dr.service.AssetService;
import com.ericsson.bos.dr.service.utils.Groovy;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import groovy.lang.Script;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Groovy filter condition which executes a custom filter defined as an inline groovy expression or
 * a groovy script.
 * The groovy expression or script must return a valid boolean result.
 */
@Component
public abstract class ScriptCondition implements Condition {

    @Autowired
    private AssetService assetService;

    /**
     * Evaluate the groovy expression or script.
     * @param filterDef filter definition
     * @param filterCtx filter context
     * @param scriptArgs script arguments
     * @return evaluation result, true or false
     */
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx, final Map<String, Object> scriptArgs) {
        final String expression = filterDef.getCondition().getArg();

        final Object result;
        if (expression.startsWith("@")) {
            final var scriptName = expression.substring(1);
            final Class<Script> scriptClass = assetService.getGroovyScript(scriptName, filterCtx.getFeaturePackId());
            result = Groovy.evalScript(scriptClass, scriptArgs);
        } else {
            result = Groovy.evalExpression(expression, scriptArgs);
        }

        if (result == null) {
            return false;
        }  else if (result instanceof String) {
            return Boolean.valueOf(result.toString());
        } else {
            return (boolean) result;
        }
    }
}