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

/**
 * A job to be executed by the JobExecutor.
 */
public abstract class ExecutableJob implements Runnable {

    private final JobEntity jobEntity;

    /**
     * ExecutableJob.
     * @param jobEntity job entity
     */
    ExecutableJob(JobEntity jobEntity) {
        this.jobEntity = jobEntity;
    }

    /**
     * Execute the job.
     * @param jobEntity job entity.
     */
    abstract void executeJob(JobEntity jobEntity);

    @Override
    public void run() {
        executeJob(jobEntity);
    }

    public JobEntity getJobEntity() {
        return jobEntity;
    }
}