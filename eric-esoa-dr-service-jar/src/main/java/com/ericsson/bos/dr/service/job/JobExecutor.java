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

import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.NEW;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_REQUESTED;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.JobService;

/**
 * Execute discovery and reconcile jobs.
 * Discovery job is executed for jobs in state NEW and reconcile job is executed for jobs in state RECONCILE_REQUESTED.
 * The jobs are executed in the configured jobExecutor thread pool.
 * Each of the tasks in the job are executed in separate taskExecutor thread pool.
 */
@Component
public class JobExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutor.class);

    @Autowired
    private JobService jobService;

    @Autowired
    @Qualifier("jobsExecutor")
    private ThreadPoolTaskExecutor jobTaskExecutor;

    @Autowired
    private ObjectProvider<ExecutableJob> executableJobProvider;

    /**
     * Execute a discovery or reconcile job in the job executor thread pool for each of
     * the jobEntities.
     * The triggered Discovery or Reconcile jobs are responsible for unlocking the job after completion (success or failed).
     * If the job queue is full then the task is rejected and the job is unlocked, so it
     * can be executed at a later date.
     * This method returns once each of the jobs has been submitted to the job executor pool.
     * @param jobEntities job entities
     */
    public void execute(final List<JobEntity> jobEntities) {
        final List<ExecutableJob> executableJobs = createExecutableJobs(jobEntities);
        for (final ExecutableJob executableJob : executableJobs) {
            final var jobEntity = executableJob.getJobEntity();
            try {
                CompletableFuture.runAsync(executableJob, jobTaskExecutor)
                        .exceptionally(thrown -> {
                            LOGGER.error("Error executing job " + jobEntity.getId(), thrown);
                            return null;
                        });
            } catch (final TaskRejectedException e) {
                LOGGER.debug("Job {} rejected. Job queue is full.", jobEntity.getId());
                jobService.unlockJob(jobEntity.getId());
            }
        }
    }

    /**
     * Get the remaining queue capacity.
     * @return the number of free slots in the queue
     */
    public int getRemainingQueueCapacity() {
        return jobTaskExecutor.getQueueCapacity() - jobTaskExecutor.getQueueSize();
    }

    private List<ExecutableJob> createExecutableJobs(final List<JobEntity> jobEntities) {
        return jobEntities.stream()
                .filter(j -> NEW.toString().equals(j.getJobStatus()) ||
                        RECONCILE_REQUESTED.toString().equals(j.getJobStatus()) || "SCHEDULED".equals(j.getJobStatus()))
                .map(j -> executableJobProvider.getObject(j))
                .collect(Collectors.toList());
    }

    /**
     * Listener for ApplicationContextCLosed events.
     * Calls shutdown() on the ThreadPoolTaskExecutor to enable gracefully shutdown
     *
     * The call to the shutdown() on the ThreadPoolTaskExecutor, combined with the configuration of awaitTermination and awaitTerminationSeconds
     * in the <code>ExecutorsConfiguration</code> class ensure that a graceful termination of the ThreadPoolTaskExecutor thread is enabled.
     */
    @EventListener(ContextClosedEvent.class)
    public void closeApplication() {
        jobTaskExecutor.shutdown();
    }


}