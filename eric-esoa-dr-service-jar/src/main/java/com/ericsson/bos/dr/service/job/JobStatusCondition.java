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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Blocking conditions to support waiting for a job to transition to specific state(s).
 */
@Component
public class JobStatusCondition {

    private static final Map<Long, JobStatusLatch> jobLatches = new HashMap<>();

    private JobStatusCondition() {}

    /**
     * Block until jobs has reached one of the specified states.
     * @param jobId jobId
     * @param jobStatuses job statuses
     */
    public static void awaitJobInState(long jobId, Set<JobSummaryDto.StatusEnum> jobStatuses) {
        if (jobLatches.containsKey(jobId)) {
            throw new IllegalArgumentException("Already waiting for job " + jobId);
        }
        try {
            final var jobLatch = new JobStatusLatch(jobStatuses);
            jobLatches.put(jobId, jobLatch);
            jobLatch.await();
        } finally {
            jobLatches.remove(jobId);
        }
    }

    private static class JobStatusLatch {

        private final Set<JobSummaryDto.StatusEnum> jobStatuses;
        private final CountDownLatch countDownLatch;

        private JobStatusLatch(Set<JobSummaryDto.StatusEnum> jobStatuses) {
            this.jobStatuses = jobStatuses;
            this.countDownLatch = new CountDownLatch(1);
        }

        void await() {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void notify(final String jobStatus) {
            if (jobStatuses.contains(JobSummaryDto.StatusEnum.valueOf(jobStatus))) {
                countDownLatch.countDown();
            }
        }
    }

    /**
     * Listen for Job events and notify JobStatusLatch if there
     * is a latch associated with the updated job id.
     *
     * @param jobEvent job event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void processJobEntityUpdateEvent(JobEventPublisher.JobEvent jobEvent) {
        if (jobLatches.containsKey(jobEvent.getJobId())) {
            final var jobLatch = jobLatches.get(jobEvent.getJobId());
            jobLatch.notify(jobEvent.getStatus());
        }
    }
}