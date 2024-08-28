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

import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.COMPLETED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.PARTIALLY_RECONCILED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_FAILED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_INPROGRESS;

import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;

/**
 * Evaluates the status of a <code>Job</code>.
 */
public class JobStatusEvaluator {

    private JobStatusEvaluator() {}

    /**
     * Evaluate the status of the <code>Job</code> based on the current
     * discovered object counts.
     * <ul>
     *     <li>State is DISCOVERED if all discovered objects are discovered.</li>
     *     <li>State is COMPLETED if all discovered objects are reconciled.</li>
     *     <li>State is RECONCILED_FAILED if one or more discovered objects are failed
     *     and there are no reconciled/partially-reconcile objects.</li>
     *     <li>State is PARTIALLY_RECONCILED if some but not all objects are RECONCILED or there is one
     *     or more objects partially reconciled.</li>
     *     <li>State is RECONCILE_INPROGRESS if one or more discovered objects are reconciling.</li>
     * </ul>
     * @param discoveredObjectStatusCounts discovered object status counts
     * @return job status
     */
    public static String evaluate(DiscoveredObjectStatusCounts discoveredObjectStatusCounts) {
        final JobSummaryDto.StatusEnum jobStatus;
        if (isDiscovered(discoveredObjectStatusCounts)) {
            jobStatus = DISCOVERED;
        } else if (isCompleted(discoveredObjectStatusCounts)) {
            jobStatus = COMPLETED;
        } else if (isReconcileFailed(discoveredObjectStatusCounts)) {
            jobStatus = RECONCILE_FAILED;
        } else if (isPartiallyReconciled(discoveredObjectStatusCounts)) {
            jobStatus = PARTIALLY_RECONCILED;
        } else if (isReconcileInProgress(discoveredObjectStatusCounts)) {
            jobStatus = RECONCILE_INPROGRESS;
        } else {
            jobStatus = DISCOVERED;
        }
        return jobStatus.name();
    }

    private static boolean isDiscovered(final DiscoveredObjectStatusCounts counts) {
        return counts.getTotalStatuses() == 1 && counts.getDiscoveredCount() > 0;
    }

    private static boolean isCompleted(final DiscoveredObjectStatusCounts counts) {
        return counts.getTotalStatuses() == 1 && counts.getReconciledCount() > 0;
    }

    private static boolean isReconcileFailed(final DiscoveredObjectStatusCounts counts) {
        return counts.getReconcileFailedCount() > 0 &&
                counts.getReconciledCount() == 0 && counts.getPartiallyReconciledCount() == 0;
    }

    private static boolean isPartiallyReconciled(final DiscoveredObjectStatusCounts counts) {
        return counts.getReconcilingCount() == 0 &&
                ((counts.getReconciledCount() > 0 && counts.getTotalStatuses() > 1) || counts.getPartiallyReconciledCount() > 0);
    }

    private static boolean isReconcileInProgress(final DiscoveredObjectStatusCounts counts) {
        return counts.getReconcilingCount() > 0;
    }
}