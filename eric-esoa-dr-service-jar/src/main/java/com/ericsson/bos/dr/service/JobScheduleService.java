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
package com.ericsson.bos.dr.service;

import static com.ericsson.bos.dr.jpa.OffsetPageRequest.DEFAULT_OFFSET;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.INVALID_CRON_EXPRESSION;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_SCHEDULE_EXISTS;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_SCHEDULE_NOT_FOUND;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.ericsson.bos.dr.jpa.JobRepository;
import com.ericsson.bos.dr.jpa.JobScheduleRepository;
import com.ericsson.bos.dr.jpa.JobSpecificationRepository;
import com.ericsson.bos.dr.jpa.OffsetPageRequest;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import com.ericsson.bos.dr.jpa.model.JobSpecificationEntity;
import com.ericsson.bos.dr.model.mappers.JobScheduleEntityMapper;
import com.ericsson.bos.dr.model.mappers.JobScheduleMapper;
import com.ericsson.bos.dr.model.mappers.JobScheduleProperties;
import com.ericsson.bos.dr.model.mappers.JobScheduleSortMapper;
import com.ericsson.bos.dr.model.mappers.JobScheduleSummaryMapper;
import com.ericsson.bos.dr.model.mappers.SpecificationMapper;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.web.v1.api.model.CreateJobScheduleDto;
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleDto;
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleListDto;
import com.ericsson.bos.dr.web.v1.api.model.JobScheduleSummaryDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Managed Job Schedules.
 */
@Service
public class JobScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduleService.class);

    @Value("${service.pagination.default_limit}")
    private int defaultLimit;

    @Autowired
    private JobScheduleRepository jobScheduleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSpecificationRepository jobSpecificationRepository;

    @Autowired
    private JobService jobService;

    /**
     * Get all job schedules with filtering and pagination.
     *
     * @param offset  page offset, defaults to 0 if null or not an int
     * @param limit   page limit, defaults to 100 if null or not an int
     * @param sort    page sort by attribute and direction
     * @param filters filters
     * @return JobScheduleListDto
     */
    @Transactional(readOnly = true)
    public JobScheduleListDto getJobSchedules(String offset, String limit, String sort, String filters) {
        final var pageRequest = new OffsetPageRequest(toInt(offset, DEFAULT_OFFSET), toInt(limit, defaultLimit),
                new JobScheduleSortMapper().apply(sort));

        final Page<JobScheduleEntity> jobSchedules;
        if (StringUtils.isEmpty(filters)) {
            jobSchedules = jobScheduleRepository.findAll(pageRequest);
        } else {
            final Specification<JobScheduleEntity> specification =
                    new SpecificationMapper<JobScheduleEntity>(new JobScheduleProperties()).apply(filters);
            jobSchedules = jobScheduleRepository.findAll(specification, pageRequest);
        }

        final List<JobScheduleSummaryDto> jobScheduleSummaryDtos = StreamSupport.stream(jobSchedules.spliterator(), false)
                .map(entity -> new JobScheduleSummaryMapper().apply(entity))
                .toList();

        return new JobScheduleListDto().items(jobScheduleSummaryDtos).totalCount((int) jobSchedules.getTotalElements());
    }

    /**
     * Get a job schedule identified by its id.
     *
     * @param jobScheduleId job schedule id
     * @return FeaturePackDto
     */
    @Transactional(readOnly = true)
    public JobScheduleDto getJobSchedule(final String jobScheduleId) {
        return new JobScheduleMapper().apply(findJobScheduleById(jobScheduleId));
    }

    /**
     * Delete a job schedule identified by its id.
     * If the associated job is in state SCHEDULED then it will be deleted. If the scheduled job is running,
     * then the job will be left and the schedule deleted.
     * If there are no jobs associated with the schedule, then the job specification will also be deleted.
     * @param jobScheduleId job schedule id
     */
    @Transactional
    public void deleteJobSchedule(final String jobScheduleId) {
        final JobScheduleEntity jobScheduleEntity = findJobScheduleById(jobScheduleId);
        deleteScheduledJob(jobScheduleEntity.getId());
        jobScheduleRepository.deleteById(findJobScheduleById(jobScheduleId).getId());
        if (jobRepository.countByJobScheduleId(jobScheduleEntity.getId()) == 0) {
            jobSpecificationRepository.deleteById(jobScheduleEntity.getJobSpecification().getId());
        }
    }

    /**
     * Delete all job schedules associated with the specified feature pack.
     * @param featurePackId feature pack id
     */
    @Transactional
    public void deleteFeaturePackJobSchedules(final String featurePackId) {
        final Specification<JobScheduleEntity> specification = (root, query, builder) -> builder.equal(root.get("jobSpecification").get(
                "featurePackId"), featurePackId);
        final List<JobScheduleEntity> jobSchedules = jobScheduleRepository.findAll(specification);
        if (!jobSchedules.isEmpty()) {
            jobSchedules.forEach(js -> deleteJobSchedule(js.getId().toString()));
        }
    }

    /**
     * Enable or disable a job schedule.
     * If schedule is being enabled, then a new scheduled job is created in SCHEDULED state.
     * If schedule is being disabled, the existing scheduled job in state SCHEDULED is deleted if existing. If the
     * scheduled job is running then it will not be affected and will run to completion.
     * @param jobScheduleId job schedule id
     */
    @Transactional
    public void enableJobSchedule(final String jobScheduleId, final boolean enabled) {
        final JobScheduleEntity jobScheduleEntity = findJobScheduleById(jobScheduleId);
        if (jobScheduleEntity.isEnabled() != enabled) {
            LOGGER.info("Setting Job schedule enabled to '{}' for schedule '{}'", enabled, jobScheduleEntity.getName());
            jobScheduleEntity.setEnabled(enabled);
            jobScheduleRepository.save(jobScheduleEntity);
            final List<JobEntity> scheduledJobs = jobRepository.findByJobScheduleIdAndJobStatus(jobScheduleEntity.getId(), "SCHEDULED");
            if (enabled && scheduledJobs.isEmpty()) {
                enableJobSchedule(jobScheduleEntity, jobScheduleId);
            } else if (!enabled && !scheduledJobs.isEmpty()) {
                disableJobSchedule(scheduledJobs);
            }
        } else {
            LOGGER.info("Job schedule enabled is already {} for schedule {}", enabled, jobScheduleId);
        }
    }

    /**
     * Create job schedule, resulting in the creation of a SCHEDULED job with due date set
     * based on the configured cron expression.
     * Creation will fail if the request contains missing or incorrect parameters, such as
     * invalid cron expression.
     * @param createJobScheduleDto job schedule creation details.
     * @return JobScheduleDto
     */
    @Transactional
    public JobScheduleDto createJobSchedule(final CreateJobScheduleDto createJobScheduleDto) {
        jobScheduleRepository.findByName(createJobScheduleDto.getName())
                .ifPresent(entity -> { throw new DRServiceException(JOB_SCHEDULE_EXISTS, createJobScheduleDto.getName()); } );
        if (!CronExpression.isValidExpression(createJobScheduleDto.getExpression())) {
            throw new DRServiceException(INVALID_CRON_EXPRESSION, createJobScheduleDto.getExpression());
        }
        // create JobEntity first. this will validate the jobSpecification properties before creating the schedule.
        final JobEntity scheduledJobEntity = jobService.createJobEntity(createJobScheduleDto.getJobSpecification());
        LOGGER.info("Creating job schedule: {}", createJobScheduleDto);
        final JobScheduleEntity jobScheduleEntity = new JobScheduleEntityMapper(scheduledJobEntity.getJobSpecification()).apply(createJobScheduleDto);
        final JobScheduleEntity savedJobScheduleEntity = jobScheduleRepository.save(jobScheduleEntity);
        scheduledJobEntity.scheduleJob(savedJobScheduleEntity);
        jobRepository.save(scheduledJobEntity);
        return new JobScheduleMapper().apply(savedJobScheduleEntity);
    }

    /**
     * Create the next scheduled job if the schedule still exists and is enabled.
     * The JobEntity is created in state SCHEDULED with due date set to the next execution time based on the configured cron expression.
     * @param scheduledJobEntity current scheduled job entity.
     * @return JobEntity or null if the schedule no longer exists or is disabled
     */
    @Transactional
    public JobEntity createNextScheduledJob(final JobEntity scheduledJobEntity) {
        final Optional<JobScheduleEntity> jobScheduleEntity = jobScheduleRepository.findById(scheduledJobEntity.getJobScheduleId());
        if (jobScheduleEntity.isPresent() && jobScheduleEntity.get().isEnabled()) {
            final JobEntity jobEntity = new JobEntity();
            jobEntity.scheduleJob(jobScheduleEntity.get());
            // re-read JobSpecification as scheduleJobEntity will be detached
            final JobSpecificationEntity jobSpecificationEntity =
                    jobSpecificationRepository.findById(scheduledJobEntity.getJobSpecification().getId())
                    .orElseThrow(() -> new IllegalStateException("Job specification not found: " + scheduledJobEntity.getJobSpecification().getId()));
            jobEntity.setJobSpecification(jobSpecificationEntity);
            return jobRepository.save(jobEntity);
        }
        return null;
    }

    /**
     * Find <code>JobScheduleEntity</code> by its id.
     * If not found, then <code>DrServiceException</code> is thrown.
     * @param jobScheduleId job schedule id
     * @return JobScheduleEntity
     */
    public JobScheduleEntity findJobScheduleById(final String jobScheduleId) {
        return jobScheduleRepository.findById(Long.valueOf(jobScheduleId))
                .orElseThrow(() -> new DRServiceException(JOB_SCHEDULE_NOT_FOUND, jobScheduleId));
    }

    private void deleteScheduledJob(final Long jobScheduleId) {
        final List<JobEntity> scheduledJobs = jobRepository.findByJobScheduleIdAndJobStatus(jobScheduleId, "SCHEDULED");
        if (!scheduledJobs.isEmpty()) {
            LOGGER.debug("Deleting scheduled job {}", scheduledJobs.get(0).getId());
            jobRepository.deleteById(scheduledJobs.get(0).getId());
        }
    }

    private void enableJobSchedule(final JobScheduleEntity jobScheduleEntity, final String jobScheduleId) {
        final List<JobEntity> inprogressJobs = jobRepository.findByJobScheduleIdAndJobStatusIn(jobScheduleEntity.getId(), Arrays.asList(
                StatusEnum.DISCOVERY_INPROGRESS.toString(), StatusEnum.RECONCILE_INPROGRESS.toString()));
        if (!inprogressJobs.isEmpty()) {
            throw new DRServiceException(ErrorCode.CANNOT_ENABLE_JOB_SCHEDULE, jobScheduleId, inprogressJobs.get(0).getId().toString());
        }
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setJobSpecification(jobScheduleEntity.getJobSpecification());
        jobEntity.scheduleJob(jobScheduleEntity);
        final JobEntity savedJobEntity = jobRepository.save(jobEntity);
        LOGGER.info("Created scheduled job: {}", savedJobEntity.getId());
    }

    private void disableJobSchedule(final List<JobEntity> scheduledJobs) {
        jobRepository.deleteAll(scheduledJobs); // should only be 1 but delete all to cover unexpected occurrences
        LOGGER.info("Deleted scheduled job: {}", scheduledJobs.stream().map(j -> j.getId()).toList());
    }
}