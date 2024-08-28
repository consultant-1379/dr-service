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
package com.ericsson.bos.dr.service.discovery.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;

/**
 * The available substitution context during discovery.
 */
public class DiscoverySubstitutionCtx implements Supplier<Map<String, Object>> {

    private final DiscoveryContext discoveryContext;
    private final DiscoveredObject discoveredObject;

    /**
     * Constructor.
     *
     * @param discoveryContext
     *         discovery context
     */
    public DiscoverySubstitutionCtx(final DiscoveryContext discoveryContext) {
        this(discoveryContext, null);
    }

    /**
     * Constructor.
     *
     * @param discoveryContext
     *         discovery context
     * @param discoveredObject
     *         discovered object
     */
    public DiscoverySubstitutionCtx(final DiscoveryContext discoveryContext, final DiscoveredObject discoveredObject) {
        this.discoveryContext = discoveryContext;
        this.discoveredObject = discoveredObject;
    }

    @Override
    public Map<String, Object> get() {
        final Map<String, Object> substitutionCtx = new HashMap<>();
        substitutionCtx.put("inputs", discoveryContext.getInputs());
        substitutionCtx.put("job", getJobExecutionContext());
        Optional.ofNullable(discoveredObject).ifPresent(
                o -> substitutionCtx.put(discoveredObject.getType().name().toLowerCase(), discoveredObject.getProperties()));
        return substitutionCtx;
    }

    private Map<String, Object> getJobExecutionContext() {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("id", discoveryContext.getJobId());
        ctx.put("name", discoveryContext.getJobName());
        ctx.put("featurePack", Map.of("id", discoveryContext.getFeaturePackId(), "name", discoveryContext.getFeaturePackName()));
        return ctx;
    }
}
