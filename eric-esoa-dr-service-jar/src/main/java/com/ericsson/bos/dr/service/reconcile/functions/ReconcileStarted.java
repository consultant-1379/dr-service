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

import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Update model when reconciliation is started.
 */
@Component
@ReconcileFunction
public class ReconcileStarted implements Consumer<ReconcileContext> {

    @Autowired
    private JobService jobService;

    @Override
    public void accept(ReconcileContext reconcileContext) {
        jobService.setJobInProgress(reconcileContext.getJobId(), StatusEnum.RECONCILE_INPROGRESS.name());
    }
}