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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner;

/**
 * The available substitution context during reconcile.
 */
public class ReconcileSubstitutionCtx implements Supplier<Map<String, Object>> {

    private static final String INPUT_KEY = "inputs";
    private static final String SOURCE_KEY = "source";
    private static final String TARGET_KEY = "target";
    private static final String JOB = "job";

    private final DiscoveryObjectEntity discoveryObjectEntity;
    private final ReconcileContext reconcileContext;
    private final ExecuteReconcileDtoObjectsInner reconcileObject;

    /**
     * ReconcileSubstitutionCtx.
     * @param discoveryObjectEntity discovered object entity
     * @param reconcileContext reconcile context
     * @param reconcileObject object being reconciled
     */
    public ReconcileSubstitutionCtx(final DiscoveryObjectEntity discoveryObjectEntity,
                                    final ReconcileContext reconcileContext,
                                    final ExecuteReconcileDtoObjectsInner reconcileObject) {
        this.discoveryObjectEntity = discoveryObjectEntity;
        this.reconcileContext = reconcileContext;
        this.reconcileObject = reconcileObject;
    }

    @Override
    public Map<String, Object> get() {
        final Map<String, Object> substitutionCtx = new HashMap<>();
        substitutionCtx.put(INPUT_KEY, mergeInputs());
        substitutionCtx.put(SOURCE_KEY, discoveryObjectEntity.getSourceProperties());
        substitutionCtx.put(TARGET_KEY, discoveryObjectEntity.getTargetProperties());
        substitutionCtx.put(JOB, getJobExecutionContext());
        return substitutionCtx;
    }

    private Map<String, Object> mergeInputs() {
        final Map<String, Object> inputs = new HashMap<>();
        Optional.ofNullable(reconcileContext.getInputs()).ifPresent(inputs::putAll);
        Optional.ofNullable(reconcileObject.getInputs()).ifPresent(inputs::putAll);
        return inputs;
    }

    private Map<String, Object> getJobExecutionContext() {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("id", reconcileContext.getJobId());
        ctx.put("name", reconcileContext.getJobName());
        ctx.put("featurePack", Map.of("id", reconcileContext.getFeaturePackId(), "name", reconcileContext.getFeaturePackName()));
        return ctx;
    }
}