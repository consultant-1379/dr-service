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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.service.AssetService;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * PostFunction step which performs substitution of the defined 'postFunction' using the
 * original command response string as the substitution context. The command response
 * string is made accessible via the key 'originalOutputs'.
 */
@Component
public class PostFunctionStep implements ExecutionStep<String, String> {

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Autowired
    private AssetService assetService;

    @Override
    public String execute(String substitutionInput, ExecutionContext executionContext) {
        return Optional.ofNullable(executionContext.getActionDto().getPostFunction())
                .map(f -> applyPostFunction(f, substitutionInput, executionContext))
                .orElse(substitutionInput);
    }

    private String applyPostFunction(final String postFunction, final String substitutionInput, ExecutionContext executionContext) {
        final String template = postFunction.startsWith("@") ?
                getAsset(postFunction.substring(1), executionContext.getFeaturePackId()) : postFunction;
        final Map<String, Object> substitutionCtx = Collections.singletonMap("originalOutputs", substitutionInput);
        final String substitutedPostFunction = substitutionEngine.render(template, substitutionCtx, executionContext.getFeaturePackId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("PostFunction output: {}", substitutedPostFunction);
        }
        return substitutedPostFunction;
    }

    private String getAsset(String postFunction, final long featurePackId) {
        return new String(assetService.getAssetContent(postFunction, featurePackId), StandardCharsets.UTF_8);
    }
}