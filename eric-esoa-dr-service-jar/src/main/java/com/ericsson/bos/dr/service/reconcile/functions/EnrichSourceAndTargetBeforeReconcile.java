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

import java.util.Optional;
import java.util.function.Consumer;

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.ExecutionEngine;
import com.ericsson.bos.dr.service.execution.ExecutionResult;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Enrich a source object and its target after discovery based on the enrichment configuration defined in the
 * reconciliation job.
 */
@Component
@ReconcileFunction
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EnrichSourceAndTargetBeforeReconcile implements Consumer<ReconcileContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichSourceAndTargetBeforeReconcile.class);

    @Autowired
    private ExecutionEngine executionEngine;

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    @Autowired
    private ReconcileStateHandler reconcileStateHandler;

    private ExecuteReconcileDtoObjectsInner reconcileObject;

    @Override
    public void accept(ReconcileContext reconcileContext) {
        final Optional<ApplicationConfigurationActionDto> enrichSourceAction = reconcileContext.getSourceEnrichAction();
        final Optional<ApplicationConfigurationActionDto> enrichTargetAction = reconcileContext.getTargetEnrichAction();

        if (!enrichSourceAction.isPresent() && !enrichTargetAction.isPresent()) {
            return;
        }

        final var discoveryObjectEntity =
                discoveryObjectRepository.findById(Long.valueOf(reconcileObject.getObjectId()))
                        .orElseThrow(() -> new IllegalStateException("Discovery object not found"));

        if (enrichSourceAction.isPresent()) {
            LOGGER.info("Executing source object enrichment action: jobId={}, objectProperties={}", reconcileContext.getJobId(),
                    discoveryObjectEntity.getSourceProperties());
            final var executionResult = execute(enrichSourceAction.get(), reconcileContext,
                    reconcileObject, discoveryObjectEntity);
            discoveryObjectEntity.getSourceProperties().putAll(executionResult.getMappedCommandResponse().get(0));
        }

        if (enrichTargetAction.isPresent()) {
            LOGGER.info("Executing target object enrichment action: jobId={}, objectProperties={}", reconcileContext.getJobId(),
                    discoveryObjectEntity.getTargetProperties());
            final var executionResult = execute(enrichTargetAction.get(), reconcileContext,
                    reconcileObject, discoveryObjectEntity);
            discoveryObjectEntity.getTargetProperties().putAll(executionResult.getMappedCommandResponse().get(0));
        }

        discoveryObjectRepository.save(discoveryObjectEntity);
    }

    private ExecutionResult execute(final ApplicationConfigurationActionDto action,
                                    final ReconcileContext reconcileContext,
                                    final ExecuteReconcileDtoObjectsInner reconcileObject,
                                    final DiscoveryObjectEntity discoveryObjectEntity) {
        try {
            final var substitutionCtx = new ReconcileSubstitutionCtx(
                    discoveryObjectEntity, reconcileContext, reconcileObject).get();
            final var executionContext = new ExecutionContext(reconcileContext.getFeaturePackId(), action, substitutionCtx);
            return executionEngine.execute(executionContext);
        } catch (final Exception e) {
            reconcileStateHandler.setDiscoveryObjectStateFailed(discoveryObjectEntity, e);
            throw e;
        }
    }

    public void setReconcileObject(ExecuteReconcileDtoObjectsInner reconcileObject) {
        this.reconcileObject = reconcileObject;
    }
}