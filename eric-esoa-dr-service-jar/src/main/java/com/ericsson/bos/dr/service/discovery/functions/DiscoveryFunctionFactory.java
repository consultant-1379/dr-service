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

import java.util.function.Consumer;

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <code>DiscoveryFunction</code> factory.
 */
@Component
public class DiscoveryFunctionFactory {

    @Autowired
    private Provider<DiscoveryFunctions> discoveryFunctions;

    public Consumer<DiscoveryContext> getValidateInputs() {
        return getDiscoveryFunction(ValidateInputs.class);
    }

    public Consumer<DiscoveryContext> getFetchSources() {
        return getDiscoveryFunction(FetchSources.class);
    }

    public Consumer<DiscoveryContext> getFetchTargets() {
        return getDiscoveryFunction(FetchTargets.class);
    }

    public Consumer<DiscoveryContext> getSaveDiscoveryObjects() {
        return getDiscoveryFunction(SaveDiscoveryObjects.class);
    }

    public Consumer<DiscoveryContext> getDiscoveryCompleted() {
        return getDiscoveryFunction(DiscoveryCompleted.class);
    }

    public Consumer<DiscoveryContext> getDiscoveryFailed() {
        return getDiscoveryFunction(DiscoveryFailed.class);
    }

    /**
     * Get <code>EnrichDiscoveredObject</code> function.
     * @param discoveredObject discovered object
     * @return EnrichDiscoveredObject discovery function
     */
    public Consumer<DiscoveryContext> getEnrichDiscoveryObject(final DiscoveredObject discoveredObject) {
        final Consumer<DiscoveryContext> function = getDiscoveryFunction(EnrichDiscoveryObject.class);
        ((EnrichDiscoveryObject)function).setDiscoveredObject(discoveredObject);
        return function;
    }

    public Consumer<DiscoveryContext> getCompareSourcesAndTargets() {
        return getDiscoveryFunction(CompareSourcesAndTargets.class);
    }

    public Consumer<DiscoveryContext> getLinkSourcesAndTargets() {
        return getDiscoveryFunction(LinkSourceAndTarget.class);
    }

    private Consumer<DiscoveryContext> getDiscoveryFunction(final Class<?> clazz) {
        return discoveryFunctions.get().getFunctions().stream().filter(f -> f.getClass().equals(clazz)).findAny()
                .orElseThrow(() -> new IllegalStateException("Discovery function not found:" + clazz.getName()));
    }

}