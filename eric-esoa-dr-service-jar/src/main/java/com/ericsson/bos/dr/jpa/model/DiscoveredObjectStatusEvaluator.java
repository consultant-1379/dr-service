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
package com.ericsson.bos.dr.jpa.model;

import static com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction.StatusEnum.COMPLETED;
import static com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction.StatusEnum.FAILED;
import static com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction.StatusEnum.INPROGRESS;
import static com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction.StatusEnum.NOT_STARTED;
import java.util.Set;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto;

/**
 * Evaluates the status of a <code>DiscoveredObject</code>.
 */
public class DiscoveredObjectStatusEvaluator {

    private DiscoveredObjectStatusEvaluator() {}

    /**
     * Evaluate the status of the <code>DiscoveredObject</code> based on the current
     * status of the filter actions.
     * <ul>
     *     <li>State is RECONCILED if all filter actions are in state COMPLETED.</li>
     *     <li>State is RECONCILED_FAILED if one or more actions are in state FAILED and no other actions are started.</li>
     *     <li>State is PARTIALLY_RECONCILED if one ore more actions are COMPLETED and there are actions which have not yet completed.</li>
     *     <li>State is RECONCILING if any action is INPROGRESS</li>
     * </ul>
     * @param discoveryObjectEntity discovered object
     * @return discovered object status
     */
    public static String evaluate(DiscoveryObjectEntity discoveryObjectEntity) {
        final Set<String> reconcileStatuses = discoveryObjectEntity.getFilters().stream()
                .map(f -> f.getReconcileStatus()).collect(Collectors.toSet());
        final DiscoveredObjectDto.StatusEnum discoveryObjectStatus;
        if (isReconcileCompleted(reconcileStatuses)) {
            discoveryObjectStatus = DiscoveredObjectDto.StatusEnum.RECONCILED;
        } else if (isReconcileFailed(reconcileStatuses)) {
            discoveryObjectStatus = DiscoveredObjectDto.StatusEnum.RECONCILE_FAILED;
        } else if (isPartiallyReconciled(reconcileStatuses)) {
            discoveryObjectStatus = DiscoveredObjectDto.StatusEnum.PARTIALLY_RECONCILED;
        } else if (isReconciling(reconcileStatuses)) {
            discoveryObjectStatus = DiscoveredObjectDto.StatusEnum.RECONCILING;
        } else {
            discoveryObjectStatus = DiscoveredObjectDto.StatusEnum.DISCOVERED;
        }
        return discoveryObjectStatus.toString();
    }

    private static boolean isReconcileCompleted(Set<String> reconcileActionStatuses) {
        return reconcileActionStatuses.size() == 1 && reconcileActionStatuses.contains(COMPLETED.toString());
    }

    private static boolean isReconcileFailed(Set<String> reconcileActionStatuses) {
        return (reconcileActionStatuses.size() == 1 && reconcileActionStatuses.contains(FAILED.toString())) ||
                reconcileActionStatuses.size() == 2 && reconcileActionStatuses.contains(FAILED.toString()) &&
                        reconcileActionStatuses.contains(NOT_STARTED.toString());
    }

    private static boolean isPartiallyReconciled(Set<String> reconcileActionStatuses) {
        return reconcileActionStatuses.size() > 1 && reconcileActionStatuses.contains(COMPLETED.toString())
                && !isReconciling(reconcileActionStatuses);
    }

    private static boolean isReconciling(Set<String> reconcileActionStatuses) {
        return reconcileActionStatuses.contains(INPROGRESS.toString());
    }
}