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
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import com.ericsson.bos.dr.service.JobScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Executes a scheduled job.
 * On completion of the scheduled job, a new scheduled job is created with dueDate set to
 * next execution time as per the associated job schedules cron expression. This is the case
 * provided the schedule is still enabled. If the job schedule has been deleted or disabled while the job is running,
 * then the next scheduled job will not be created.
 */
public class ScheduledJob extends DiscoveryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJob.class);

    @Autowired
    private JobScheduleService jobScheduleService;

    /**
     * ScheduledJob.
     *
     * @param jobEntity job entity
     */
    public ScheduledJob(final JobEntity jobEntity) {
        super(jobEntity);
    }

    @Override
    public void executeJob(final JobEntity jobEntity) {
        final JobScheduleEntity jobScheduleEntity = jobScheduleService.findJobScheduleById(jobEntity.getJobScheduleId().toString());
        try {
            LOGGER.info("Running job for schedule '{}' with expression '{}'", jobScheduleEntity.getName(), jobScheduleEntity.getExpression());
            super.executeJob(jobEntity);
        } finally {
            final JobEntity nextScheduledJob = jobScheduleService.createNextScheduledJob(jobEntity);
            if (nextScheduledJob != null) {
                LOGGER.info("Set next execution time for job schedule '{}' to {}", jobScheduleEntity.getName(), nextScheduledJob.getDueDate());
            } else {
                LOGGER.info("Skip creating job for next schedule. Job schedule '{}' has been deleted or disabled.",
                        jobScheduleEntity.getName());
            }
        }
    }
}