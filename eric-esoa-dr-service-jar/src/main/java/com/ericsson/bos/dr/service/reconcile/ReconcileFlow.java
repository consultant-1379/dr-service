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

package com.ericsson.bos.dr.service.reconcile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.reconcile.functions.ReconcileFunctionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Reconcile flow.
 * <ol>
 *     <li>Validate the reconcile request.</li>
 *     <li>Optionally Enrich source and target objects concurrently.</li>
 *     <li>Reconcile the specified objects concurrently.</li>
 * </ol>
 */
@Component
public class ReconcileFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconcileFlow.class);

    @Autowired
    @Qualifier("tasksExecutor")
    private Executor executor;

    @Autowired
    private ReconcileFunctionFactory reconcileFunctionFactory;

    /**
     * Execute the reconciliation flow.
     * First validates the requests. If request is valid then reconcile flow is
     * executed, otherwise a validation exception is thrown.
     * @param reconcileContext reconcile context
     * @return CompletableFuture
     */
    public CompletableFuture<Void> execute(ReconcileContext reconcileContext) {
        return reconcileStarted(reconcileContext)
                .thenCompose(x -> CompletableFuture.allOf(enrichSourcesAndTargets(reconcileContext)))
                .thenCompose(x -> CompletableFuture.allOf(reconcileTargets(reconcileContext)))
                .thenRun(reconcileCompleted(reconcileContext))
                .exceptionally(reconcileFailed(reconcileContext));
    }

    private CompletableFuture<Void> reconcileStarted(ReconcileContext reconcileContext) {
        return CompletableFuture.runAsync(() -> reconcileFunctionFactory.getReconcileStarted().accept(reconcileContext), executor);
    }

    private CompletableFuture<Void>[] enrichSourcesAndTargets(ReconcileContext reconcileContext) {
        final boolean enrichSourceActionPresent = reconcileContext.getSourceEnrichAction().isPresent();
        final boolean enrichTargetActionPresent = reconcileContext.getTargetEnrichAction().isPresent();
        if (enrichSourceActionPresent || enrichTargetActionPresent) {
            final List<CompletableFuture<Void>> sources = reconcileContext.getReconcileObjects().stream()
                    .map(s -> CompletableFuture.runAsync(() -> reconcileFunctionFactory.getEnrichSourceAndTarget(s).accept(reconcileContext),
                            executor))
                    .collect(Collectors.toList());
            return sources.toArray(CompletableFuture[]::new);
        }

        return new CompletableFuture[0];
    }

    private CompletableFuture<Void>[] reconcileTargets(ReconcileContext reconcileContext) {
        final List<CompletableFuture<Void>> sources = reconcileContext.getReconcileObjects().stream()
                .map(s -> CompletableFuture.runAsync(() -> reconcileFunctionFactory.getReconcileSingleObject(s).accept(reconcileContext),
                        executor))
                .collect(Collectors.toList());
        return sources.toArray(CompletableFuture[]::new);
    }

    private Runnable reconcileCompleted(ReconcileContext reconcileContext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reconcile Completed, featurePackId: {}, jobId: {}, reconciled objects: {}",
                    reconcileContext.getFeaturePackId(), reconcileContext.getJobId(), reconcileContext.getReconcileObjects());
        }
        return () -> reconcileFunctionFactory.getReconcileCompleted().accept(reconcileContext);
    }

    private Function<Throwable, Void> reconcileFailed(ReconcileContext reconcileContext) {
        return t -> {
            LOGGER.error("Reconcile failed", t);
            reconcileFunctionFactory.getReconcileFailed().accept(reconcileContext);
            return null;
        };
    }
}