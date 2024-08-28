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

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.model.DiscoveredObjectStatusCounts;
import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.jpa.model.StatusCount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Update model after reconciliation has completed successfully.
 */
@Component
@ReconcileFunction
public class ReconcileCompleted implements Consumer<ReconcileContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconcileCompleted.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    @Override
    public void accept(ReconcileContext reconcileContext) {
        final List<StatusCount> statusCounts = discoveryObjectRepository.getCountsGroupedByStatus(reconcileContext.getJobId());
        final DiscoveredObjectStatusCounts discoveredObjectStatusCounts = new DiscoveredObjectStatusCounts(statusCounts);
        LOGGER.info("Reconcile completed for jobId={}, counts={}", reconcileContext.getJobId(), discoveredObjectStatusCounts);
        jobService.reconcileCompleted(reconcileContext.getJobId(), discoveredObjectStatusCounts);
    }
}
