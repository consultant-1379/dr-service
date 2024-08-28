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
package com.ericsson.bos.dr.service.execution.steps;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.service.AssetService;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import com.ericsson.bos.dr.service.utils.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * PreFunction step which performs jinja substitution of the defined 'preFunction' using the
 * provided substitution context.
 * The step outputs an enhanced substitution context containing both the original context and
 * the result of the preFunction substitution accessible via key 'preFunction'.
 * The preFunction result may be the raw substitution string or converted to an Object depending on its usage in
 * the command properties. This allows to either substitute the preFunction directly as a json payload '{{preFunction}}
 * or access the properties '{{preFunction.prop1}}. The command properties are checked to determine whether the result
 * should be converted or not.
 */
@Component
public class PreFunctionStep implements ExecutionStep<Map<String, Object>, Map<String, Object>> {

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Autowired
    private AssetService assetService;

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ExecutionContext executionContext) {
        return Optional.ofNullable(executionContext.getActionDto().getPreFunction())
                .map(f -> applyPreFunction(f, input, executionContext))
                .orElse(input);
    }

    private Map<String, Object> applyPreFunction(final String preFunction, final Map<String, Object> substitutionCtx,
                                                 ExecutionContext executionContext) {
        final String template = preFunction.startsWith("@") ?
                getAsset(preFunction.substring(1), executionContext.getFeaturePackId()) : preFunction;
        final String substitutedPreFunction = substitutionEngine.render(template, substitutionCtx, executionContext.getFeaturePackId());
        final Map<String, Object> preFunctionSubstitutionCtx = new HashMap<>();
        if (shouldConvertResultToObject(executionContext)) {
            preFunctionSubstitutionCtx.put("preFunction", JSON.readObject(substitutedPreFunction));
        } else {
            preFunctionSubstitutionCtx.put("preFunction", substitutedPreFunction);
        }
        preFunctionSubstitutionCtx.putAll(substitutionCtx);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("PreFunction output: {}", preFunctionSubstitutionCtx);
        }
        return preFunctionSubstitutionCtx;
    }

    private boolean shouldConvertResultToObject(ExecutionContext executionContext) {
        final var propertiesString = JSON.toString(executionContext.getActionDto().getProperties());
        return !StringUtils.deleteWhitespace(propertiesString).contains("{{preFunction}}");
    }

    private String getAsset(String preFunction, final long featurePackId) {
        return new String(assetService.getAssetContent(preFunction, featurePackId), StandardCharsets.UTF_8);
    }
}