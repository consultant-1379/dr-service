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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.JobService;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Scheduled task to fetch executable jobs and pass to the <code>JobExecutor</code>.
 * An executable job is a job in state NEW, RECONCILE_REQUESTED  or SCHEDULED and whose locked property
 * value is false. In case of job in state SCHEDULED it is executable only if the current job acquisition time
 * is greater than or equal to the jobs due date.
 */
@Component
public class ScheduledJobAcquisitionTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobAcquisitionTask.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Autowired
    private JobService jobService;

    @Autowired
    private JobExecutor jobExecutor;

    @Value("${service.jobs.acquisition.max-jobs}")
    private int maxJobs;

    /**
     * Fetch executable jobs and pass to the JobExecutor.
     * The jobs are fetched and locked in a single transaction before being
     * passed to the JobExecutor. The fetch and update are performed
     * using SKIP LOCKED option to ensure the same jobs cannot be picked-up by
     * another running instance or transaction.
     */
    @Scheduled(fixedDelayString = "${service.jobs.acquisition.scheduler-delay}")
    synchronized void acquireJobs() {
        if (jobExecutor.getRemainingQueueCapacity() == 0) {
            LOGGER.debug("No attempt to acquire jobs, the queue is full");
            return;
        }
        try {
            isRunning.getAndSet(true);
            final int jobsLimit = Math.min(maxJobs, jobExecutor.getRemainingQueueCapacity());
            final List<JobEntity> jobEntities = jobService.findAndLockExecutableJobs(jobsLimit);
            if (jobEntities != null && !jobEntities.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Acquired {} jobs: {}", jobEntities.size(), jobEntities.stream().map(JobEntity::getId).toList());
                }
                jobExecutor.execute(jobEntities);
            }
        } catch (final Exception e) {
            LOGGER.warn("Error acquiring jobs: {}", e.getMessage());
        } finally {
            isRunning.getAndSet(false);
        }
    }

    /**
     * Process job events to trigger job acquisition outside the scheduled period
     * if not already running, in order to process a job as soon as it is available.
     * @param jobEvent job event
     */
    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void processJobEvent(final JobEventPublisher.JobEvent jobEvent) {
        if (!StatusEnum.NEW.toString().equals(jobEvent.getStatus()) &&
                !StatusEnum.RECONCILE_REQUESTED.toString().equals(jobEvent.getStatus())) {
            return;
        }
        if (!isRunning.get() && jobExecutor.getRemainingQueueCapacity() != 0) {
            LOGGER.debug("New job created, triggering job acquisition outside of scheduler period.");
            acquireJobs();
        }
    }
}