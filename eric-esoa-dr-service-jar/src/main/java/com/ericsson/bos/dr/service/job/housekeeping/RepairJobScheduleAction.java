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
package com.ericsson.bos.dr.service.job.housekeeping;

import java.sql.Date;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import com.ericsson.bos.dr.jpa.JobRepository;
import com.ericsson.bos.dr.jpa.JobScheduleRepository;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import com.ericsson.bos.dr.service.JobScheduleService;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Repair a Job Schedule which has a running job that will not complete, preventing the next
 * scheduled job from being created.
 * The running job is set to an errored state (DISCOVERY_FAILED or RECONCILE_FAILED).
 * The next scheduled job for the schedule is then created, provided the schedule still exists
 * and is enabled.
 */
@Component
public class RepairJobScheduleAction implements Consumer<JobEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepairJobScheduleAction.class);

    @Autowired
    private JobScheduleService jobScheduleService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobScheduleRepository jobScheduleRepository;

    @Override
    public void accept(JobEntity jobEntity) {
        final String erroredJobStatus = JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS.toString().equals(jobEntity.getJobStatus()) ?
                JobSummaryDto.StatusEnum.DISCOVERY_FAILED.toString() : JobSummaryDto.StatusEnum.RECONCILE_FAILED.toString();
        jobEntity.setJobStatus(erroredJobStatus);
        jobEntity.setErrorMessage(String.format("Set job state to %s after pod restart", erroredJobStatus));
        jobEntity.setCompletedDate(Date.from(Instant.now()));
        jobRepository.save(jobEntity);
        LOGGER.info("Updated state to {} for job {}", erroredJobStatus, jobEntity.getId());
        final Optional<JobScheduleEntity> jobScheduleEntity = jobScheduleRepository.findById(jobEntity.getJobScheduleId());
        if (jobScheduleEntity.isPresent() && jobScheduleEntity.get().isEnabled()) {
            final JobEntity nextScheduledJob = jobScheduleService.createNextScheduledJob(jobEntity);
            LOGGER.info("Created next job {} with due date {} for schedule {}", nextScheduledJob.getId(),
                    nextScheduledJob.getDueDate(), jobEntity.getJobScheduleId());
        }
    }
}