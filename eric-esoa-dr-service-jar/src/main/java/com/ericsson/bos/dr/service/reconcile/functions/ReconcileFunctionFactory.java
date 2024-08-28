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
package com.ericsson.bos.dr.service.reconcile.functions;

import java.util.function.Consumer;

import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <code>ReconcileFunction</code> factory.
 */
@Component
public class ReconcileFunctionFactory {

    @Autowired
    private Provider<ReconcileFunctions> reconcileFunctions;

    public Consumer<ReconcileContext> getReconcileStarted() {
        return getReconcileFunction(ReconcileStarted.class);
    }

    public Consumer<ReconcileContext> getReconcileFailed() {
        return getReconcileFunction(ReconcileFailed.class);
    }

    public Consumer<ReconcileContext> getReconcileCompleted() {
        return getReconcileFunction(ReconcileCompleted.class);
    }

    /**
     * Get <code>EnrichSourceAndTargetBeforeReconcile</code> function.
     * @param reconcileObject reconcile object
     * @return EnrichSourceAndTargetBeforeReconcile consumer function
     */
    public Consumer<ReconcileContext> getEnrichSourceAndTarget(ExecuteReconcileDtoObjectsInner reconcileObject) {

        final EnrichSourceAndTargetBeforeReconcile function =
                (EnrichSourceAndTargetBeforeReconcile) getReconcileFunction(EnrichSourceAndTargetBeforeReconcile.class);
        function.setReconcileObject(reconcileObject);
        return function;
    }

    /**
     * Get <code>ReconcileSingleObject</code> function.
     * @param reconcileObject reconcile object
     * @return ReconcileSingleObject consumer function
     */
    public Consumer<ReconcileContext> getReconcileSingleObject(ExecuteReconcileDtoObjectsInner reconcileObject) {
        final ReconcileSingleObject function = (ReconcileSingleObject) getReconcileFunction(ReconcileSingleObject.class);
        function.setReconcileObject(reconcileObject);
        return function;
    }

    private Consumer<ReconcileContext> getReconcileFunction(final Class<?> clazz) {
        return reconcileFunctions.get().getFunctions().stream()
                .filter(f -> f.getClass().equals(clazz) || (
                        f.getClass().getSuperclass() != null && f.getClass().getSuperclass().equals(clazz)))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Reconcile function not found:" + clazz.getName()));
    }
}