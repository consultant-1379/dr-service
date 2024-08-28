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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.bos.dr.jpa.JobRepository;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.DiscoveryService;
import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.service.ReconcileService;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;

/**
 * Discovery job which runs the discovery flow until completion.
 */
public class DiscoveryJob extends ExecutableJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryJob.class);

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private ReconcileService reconcileService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    /**
     * DiscoveryJob.
     *
     * @param jobEntity job entity
     */
    public DiscoveryJob(final JobEntity jobEntity) {
        super(jobEntity);
    }

    @Override
    public void executeJob(final JobEntity jobEntity) {
        jobService.setJobInProgress(jobEntity.getId(), StatusEnum.DISCOVERY_INPROGRESS.toString());
        discoveryService.executeDiscovery(jobEntity);
        final var executionOptions = jobEntity.getExecutionOptions();
        if (executionOptions != null && executionOptions.getAutoReconcile().booleanValue()) {
            jobRepository.findById(jobEntity.getId()).ifPresent(latestEntity -> {
                if (StatusEnum.DISCOVERED.toString().equals(latestEntity.getJobStatus())) {
                    LOGGER.info("autoReconcile is true, starting reconcile for job {}", jobEntity.getId());
                    reconcileService.executeReconcile(jobEntity.getId().toString(), new ExecuteReconcileDto());
                } else {
                    LOGGER.info("Skipping autoReconcile for job {}, discovery failed", jobEntity.getId());
                }
            });
        }
    }
}
