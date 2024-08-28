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

import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.DISCOVERED;
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.PARTIALLY_RECONCILED;
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILED;
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILE_FAILED;
import static com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum.RECONCILING;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <code>StatusCount</code> wrapper to get discovered object status counts.
 */
public class DiscoveredObjectStatusCounts {

    final Map<String, Integer> countsByStatus;

    /**
     * DiscoveredObjectStatusCounts
     * @param statusCounts status counts
     */
    public DiscoveredObjectStatusCounts(List<StatusCount> statusCounts) {
        this.countsByStatus = statusCounts.stream().collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));
    }

    public int getDiscoveredCount() {
        return countsByStatus.getOrDefault(DISCOVERED.name(), 0);
    }

    public int getReconciledCount() {
        return countsByStatus.getOrDefault(RECONCILED.name(), 0);
    }

    public int getReconcilingCount() {
        return countsByStatus.getOrDefault(RECONCILING.name(), 0);
    }

    public int getReconcileFailedCount() {
        return countsByStatus.getOrDefault(RECONCILE_FAILED.name(), 0);
    }

    public int getPartiallyReconciledCount() {
        return countsByStatus.getOrDefault(PARTIALLY_RECONCILED.name(), 0);
    }

    public int getTotalStatuses() {
        return countsByStatus.size();
    }

    @Override
    public String toString() {
        return countsByStatus.toString();
    }
}