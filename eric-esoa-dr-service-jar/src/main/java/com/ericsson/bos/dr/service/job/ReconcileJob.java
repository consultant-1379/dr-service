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

package com.ericsson.bos.dr.service.job;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.ReconcileService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Reconcile job which runs the reconcile flow until completion.
 */
public class ReconcileJob extends ExecutableJob {

    @Autowired
    private ReconcileService reconcileService;

    /**
     * ReconcileJob.
     * @param jobEntity job entity
     */
    public ReconcileJob(JobEntity jobEntity) {
        super(jobEntity);
    }

    @Override
    public void executeJob(final JobEntity jobEntity) {
        final var executeReconcileDto = jobEntity.getReconcileRequest();
        reconcileService.executeReconcile(jobEntity.getId().toString(), executeReconcileDto);
    }
}