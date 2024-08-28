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
package com.ericsson.bos.dr.service.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.discovery.functions.DiscoveryFunctionFactory;
import com.ericsson.bos.dr.service.utils.Futures;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Concurrent Discovery flow.
 * <ol>
 *     <li>Validate the discovery request.</li>
 *     <li>Fetch objects from source and targets system concurrently.</li>
 *     <li>Optionally Enrich source and target objects concurrently.</li>
 *     <li>Compare source and target objects to identify discrepancies.</li>
 * </ol>
 * The <code>DiscoveryContext</code> is updated throughout the flow.
 */
@Component
public class DiscoveryFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryFlow.class);

    @Autowired
    private DiscoveryFunctionFactory factory;

    @Autowired
    @Qualifier("tasksExecutor")
    private Executor executor;

    /**
     * Execute the discovery flow.
     * @param discoveryContext discovery context.
     * @return CompletableFuture
     */
    public CompletableFuture<Void> execute(DiscoveryContext discoveryContext) {
        return validateInputs(discoveryContext)
                .thenCompose(x -> CompletableFuture.allOf(fetchSources(discoveryContext), fetchTargets(discoveryContext)))
                .thenCompose(x -> Futures.allOfCancelOnFailure(enrichSourcesAndTargets(discoveryContext)))
                .thenCompose(x -> linkSourcesAndTargets(discoveryContext))
                .thenCompose(x -> compareSourcesAndTargets(discoveryContext))
                .thenCompose(x -> saveDiscoveryObjects(discoveryContext))
                .thenCompose(x -> discoveryCompleted(discoveryContext))
                .exceptionally(discoveryFailed(discoveryContext));
    }

    private CompletableFuture<Void> validateInputs(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getValidateInputs().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Validation error"));
    }

    private CompletableFuture<Void> fetchSources(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getFetchSources().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Fetch sources failed"));
    }

    private CompletableFuture<Void> fetchTargets(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getFetchTargets().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Fetch targets failed"));
    }

    private CompletableFuture<Void>[] enrichSourcesAndTargets(DiscoveryContext discoveryContext) {
        final List<DiscoveredObject> discoveredObjects = new ArrayList<>();
        discoveryContext.getDiscoverySourceEnrichAction().ifPresent(a -> discoveredObjects.addAll(discoveryContext.getSources()));
        discoveryContext.getDiscoveryTargetEnrichAction().ifPresent(a -> discoveredObjects.addAll(discoveryContext.getTargets()));
        final List<CompletableFuture<Void>> enrichmentOperations = discoveredObjects.stream()
                .map(s -> CompletableFuture.runAsync(() ->
                                        factory.getEnrichDiscoveryObject(s).accept(discoveryContext), executor)
                        .exceptionally(exceptionally(discoveryContext, "Enrichment failed")))
                .collect(Collectors.toList());
        return enrichmentOperations.toArray(CompletableFuture[]::new);
    }

    private CompletableFuture<Void> compareSourcesAndTargets(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getCompareSourcesAndTargets().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Comparison failed"));
    }

    private CompletableFuture<Void> linkSourcesAndTargets(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getLinkSourcesAndTargets().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Mapping sources to targets failed"));
    }

    private CompletableFuture<Void> saveDiscoveryObjects(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getSaveDiscoveryObjects().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Persist discovered objects failed"));
    }

    private CompletableFuture<Void> discoveryCompleted(DiscoveryContext discoveryContext) {
        return CompletableFuture.runAsync(() -> factory.getDiscoveryCompleted().accept(discoveryContext), executor)
                .exceptionally(exceptionally(discoveryContext, "Discovery completion error"));
    }

    private Function<Throwable, Void> discoveryFailed(DiscoveryContext discoveryContext) {
        return t -> {
            factory.getDiscoveryFailed().accept(discoveryContext);
            return null;
        };
    }

    private Function<Throwable, Void> exceptionally(DiscoveryContext discoveryContext, String message) {
        return t -> {
            LOGGER.error(message, t);
            discoveryContext.addException(t instanceof CompletionException ? t.getCause() : t);
            ExceptionUtils.rethrow(t);
            return null;
        };
    }
}