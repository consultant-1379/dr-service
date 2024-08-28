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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.model.DiscoveredObjectStatusEvaluator;
import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.service.execution.ExecutionEngineException;
import com.ericsson.bos.dr.service.execution.ExecutionResult;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto;
import com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction;

/**
 * Handle state updates during the reconciliation flow.
 */
@Component
public class ReconcileStateHandler {

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    /**
     * Set the filter state to failed.
     *
     * @param discoveryObjectEntity discovery object entity
     * @param filter                filter name
     * @param e                     exception
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity setFilterStateFailed(final DiscoveryObjectEntity discoveryObjectEntity, final String filter, final Exception e) {
        return discoveryObjectEntity.getFilter(filter).map(f -> {
            f.setReconcileStatus(FilterDtoReconcileAction.StatusEnum.FAILED.toString());
            if (e instanceof ExecutionEngineException executionEngineException) {
                executionEngineException.getCommand().ifPresent(f::setCommand);
                executionEngineException.getCommandOutput().ifPresent(f::setCommandResponse);
            }
            Optional.ofNullable(e.getMessage()).ifPresent(f::setErrorMsg);
            f.setErrorMsg(e.getMessage());
            discoveryObjectEntity.setStatus(DiscoveredObjectStatusEvaluator.evaluate(discoveryObjectEntity));
            return discoveryObjectRepository.save(discoveryObjectEntity);
        }).orElse(discoveryObjectEntity);
    }

    /**
     * Set filter state to completed.
     *
     * @param discoveryObjectEntity discovery object entity.
     * @param filter                filter name
     * @param executionResult       execution result
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity setFilterStateCompleted(final DiscoveryObjectEntity discoveryObjectEntity,
                                                         final String filter, final ExecutionResult executionResult) {
        return discoveryObjectEntity.getFilter(filter).map(f -> {
            f.setReconcileStatus(FilterDtoReconcileAction.StatusEnum.COMPLETED.toString());
            f.setCommand(executionResult.getCommand());
            f.setCommandResponse(executionResult.getCommandResponse());
            f.setErrorMsg(null);
            return discoveryObjectRepository.save(discoveryObjectEntity);
        }).orElse(discoveryObjectEntity);
    }

    /**
     * Set filter state to InProgress.
     *
     * @param discoveryObjectEntity discovery object entity
     * @param filter                filter name
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity setFilterStateInProgress(final DiscoveryObjectEntity discoveryObjectEntity, final String filter) {
        return discoveryObjectEntity.getFilter(filter).map(f -> {
            f.setReconcileStatus(FilterDtoReconcileAction.StatusEnum.INPROGRESS.toString());
            f.setCommand(null);
            f.setCommandResponse(null);
            f.setErrorMsg(null);
            return discoveryObjectRepository.save(discoveryObjectEntity);
        }).orElse(discoveryObjectEntity);
    }

    /**
     * Set DiscoveryObject state to Reconciling.
     *
     * @param discoveryObjectEntity discovery object entity
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity setDiscoveryObjectStateReconciling(final DiscoveryObjectEntity discoveryObjectEntity) {
        discoveryObjectEntity.setStatus(DiscoveredObjectDto.StatusEnum.RECONCILING.toString());
        discoveryObjectEntity.setErrorMessage(null);
        return discoveryObjectRepository.save(discoveryObjectEntity);
    }

    /**
     * Set DiscoveryObject state based on current state of its filters.
     *
     * @param discoveryObjectEntity discovery object entity
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity setDiscoveryObjectState(final DiscoveryObjectEntity discoveryObjectEntity) {
        discoveryObjectEntity.setStatus(DiscoveredObjectStatusEvaluator.evaluate(discoveryObjectEntity));
        return discoveryObjectRepository.save(discoveryObjectEntity);
    }

    /**
     * Set DiscoveryObject state to Reconcile Failed.
     * @param discoveryObjectEntity discovery object entity
     * @param e exception
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity setDiscoveryObjectStateFailed(final DiscoveryObjectEntity discoveryObjectEntity, final Exception e) {
        discoveryObjectEntity.setStatus(DiscoveredObjectDto.StatusEnum.RECONCILE_FAILED.toString());
        discoveryObjectEntity.setErrorMessage(e.getMessage());
        return discoveryObjectRepository.save(discoveryObjectEntity);
    }
}