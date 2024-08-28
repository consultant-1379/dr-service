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

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.jpa.model.FilterEntity;
import com.ericsson.bos.dr.service.DiscoveryService;
import com.ericsson.bos.dr.service.execution.ExecutionContext;
import com.ericsson.bos.dr.service.execution.ExecutionEngine;
import com.ericsson.bos.dr.service.execution.ExecutionResult;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationActionDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner;
import com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction.StatusEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Perform reconcile for a single discovered object.
 */
@Component
@ReconcileFunction
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReconcileSingleObject implements Consumer<ReconcileContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconcileSingleObject.class);

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private ReconcileStateHandler reconcileState;

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Autowired
    private ExecutionEngine executionEngine;

    private ExecuteReconcileDtoObjectsInner reconcileObject;

    @Override
    public void accept(ReconcileContext reconcileContext) {
        var discoveryObjectEntity = discoveryService.findDiscoveryObjectEntityById(
                Long.valueOf(reconcileObject.getObjectId()));

        if (discoveryObjectEntity == null) {
            LOGGER.info("Skipping reconcile for {}. No discovery object found.", reconcileObject.getObjectId());
            return;
        }

        if (discoveryObjectEntity.isReconcileOngoingOrCompleted()) {
            LOGGER.info("Skipping reconcile for {}. Reconcile is ongoing or completed.", reconcileObject.getObjectId());
            return;
        }

        final List<String> filters = getExecutableFilters(discoveryObjectEntity, reconcileContext);
        if (filters.isEmpty()) {
            LOGGER.info("Skipping reconcile for {}. No valid filters supplied.", reconcileObject.getObjectId());
            return;
        }

        discoveryObjectEntity = reconcileState.setDiscoveryObjectStateReconciling(discoveryObjectEntity);

        for (final String filter : filters) {
            discoveryObjectEntity = reconcileState.setFilterStateInProgress(discoveryObjectEntity, filter);
            try {
                final var reconcileAction = reconcileContext.getReconcileAction(filter);
                LOGGER.info("Executing reconcile action: objectId={},jobId={},filter={}", discoveryObjectEntity.getId(),
                        reconcileContext.getJobId(), filter);
                final var executionResult = reconcile(reconcileAction, discoveryObjectEntity,
                        reconcileContext, reconcileObject);
                discoveryObjectEntity = reconcileState.setFilterStateCompleted(discoveryObjectEntity, filter, executionResult);
            } catch (final Exception e) {
                reconcileState.setFilterStateFailed(discoveryObjectEntity, filter, e);
                throw e;
            }
        }
        reconcileState.setDiscoveryObjectState(discoveryObjectEntity);
    }

    private List<String> getExecutableFilters(final DiscoveryObjectEntity discoveryObjectEntity, final ReconcileContext reconcileContext) {
        final List<String> validFilters = discoveryObjectEntity.getFilters().stream().map(FilterEntity::getName).collect(Collectors.toList());
        final List<String> completedFilters = discoveryObjectEntity.getFilters().stream()
                .filter(f -> StatusEnum.COMPLETED.toString().equalsIgnoreCase(f.getReconcileStatus()))
                .map(FilterEntity::getName).toList();
        if (!completedFilters.isEmpty()) {
            LOGGER.info("Filters are already completed: {}", completedFilters);
            validFilters.removeAll(completedFilters);
        }
        final List<String> filters = CollectionUtils.isNotEmpty(reconcileObject.getFilters()) ?
                reconcileObject.getFilters() : reconcileContext.getFilters();
        LOGGER.debug("Filters for object {}, validFilters={}, suppliedFilters={}",
                discoveryObjectEntity.getId(), validFilters, filters);
        return filters.stream().filter(validFilters::contains).toList();
    }

    private ExecutionResult reconcile(ApplicationConfigurationActionDto reconcileAction,
                                      DiscoveryObjectEntity discoveryObjectEntity, ReconcileContext reconcileContext,
                                      ExecuteReconcileDtoObjectsInner reconcileObject) {
        final var substitutionCtx = new ReconcileSubstitutionCtx(
                discoveryObjectEntity, reconcileContext, reconcileObject).get();
        final var executionContext = new ExecutionContext(reconcileContext.getFeaturePackId(), reconcileAction, substitutionCtx);
        return executionEngine.execute(executionContext);
    }

    public void setReconcileObject(ExecuteReconcileDtoObjectsInner reconcileObject) {
        this.reconcileObject = reconcileObject;
    }
}