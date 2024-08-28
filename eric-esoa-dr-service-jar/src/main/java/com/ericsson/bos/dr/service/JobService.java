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

import static com.ericsson.bos.dr.jpa.model.JobEntity.SCHEDULED_STATUS;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_NOT_FOUND;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.COMPLETED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERY_FAILED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_INPROGRESS;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.values;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.ericsson.bos.dr.jpa.ApplicationRepository;
import com.ericsson.bos.dr.jpa.InputConfigurationsRepository;
import com.ericsson.bos.dr.jpa.JobRepository;
import com.ericsson.bos.dr.jpa.JobScheduleRepository;
import com.ericsson.bos.dr.jpa.JobSpecificationRepository;
import com.ericsson.bos.dr.jpa.OffsetPageRequest;
import com.ericsson.bos.dr.jpa.model.DiscoveredObjectStatusCounts;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.jpa.model.JobScheduleEntity;
import com.ericsson.bos.dr.jpa.model.JobStatusEvaluator;
import com.ericsson.bos.dr.model.mappers.JobEntityMapper;
import com.ericsson.bos.dr.model.mappers.JobMapper;
import com.ericsson.bos.dr.model.mappers.JobProperties;
import com.ericsson.bos.dr.model.mappers.JobProperties.JobProperty;
import com.ericsson.bos.dr.model.mappers.JobSortMapper;
import com.ericsson.bos.dr.model.mappers.JobSummaryMapper;
import com.ericsson.bos.dr.model.mappers.SpecificationMapper;
import com.ericsson.bos.dr.service.alarms.AlarmHandlerClient;
import com.ericsson.bos.dr.service.alarms.FaultName;
import com.ericsson.bos.dr.service.alarms.JobAlarmDescription;
import com.ericsson.bos.dr.service.alarms.JobAlarmResource;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.job.CreateJobValidator;
import com.ericsson.bos.dr.service.job.DeleteJobValidator;
import com.ericsson.bos.dr.service.job.FailedJobAlarmPredicate;
import com.ericsson.bos.dr.web.v1.api.model.DeleteJobsResponseDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;
import com.ericsson.bos.dr.web.v1.api.model.JobDto;
import com.ericsson.bos.dr.web.v1.api.model.JobListDto;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job Management Service.
 */
@Service
public class JobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    @Value("${service.pagination.default_limit}")
    private int defaultLimit;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSpecificationRepository jobSpecificationRepository;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private JobSummaryMapper jobSummaryMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InputConfigurationsRepository inputConfigurationsRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FeaturePackService featurePackService;

    @Autowired
    private JobScheduleRepository jobScheduleRepository;

    @Autowired
    private CreateJobValidator createJobValidator;

    @Autowired
    private AlarmHandlerClient alarmHandlerClient;

    @Autowired
    private FailedJobAlarmPredicate shouldRaiseAlarm;

    @Value("${podName}")
    private String podName;

    /**
     * Create job where at least one id or name is provided for the featurePack and application.
     * Ensures the referenced feature pack, application, job exist
     * before creating the job.
     *
     * @param executeJobDto job info to be processed
     * @return JobEntity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity createJob(final ExecuteJobDto executeJobDto) {
        final JobEntity jobEntity = createJobEntity(executeJobDto);
        return jobRepository.save(jobEntity);
    }

    /**
     * Create a job when the messageSubscriptionId is also provided.
     *
     * @param executeJobDto
     *         job info to be processed
     * @param messageSubscriptionId
     *         message subscription id
     * @return JobEntity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity createJob(final ExecuteJobDto executeJobDto, final long messageSubscriptionId) {
        final JobEntity jobEntity = createJobEntity(executeJobDto, messageSubscriptionId);
        return jobRepository.save(jobEntity);
    }

    /**
     * Creates a <code>JobEntity</code> for a new job. This creates
     * but does not save the entity. The returned entity will have no id set.
     * @see JobService#createJob(ExecuteJobDto)
     * @param executeJobDto job info to be processed
     * @return JobEntity
     */
    public JobEntity createJobEntity(final ExecuteJobDto executeJobDto) {
        return createJobEntity(executeJobDto, null);
    }

    /**
     * Get job by Id.
     *
     * @param jobId id of job to be retrieved
     * @return JobDto
     * @throws DRServiceException when job not found
     */
    @Transactional(readOnly = true)
    public JobDto getJobById(final String jobId) {
        return jobMapper.apply(findJobEntity(jobId));
    }

    /**
     * Detaches the job entity to perform a fresh read instead
     * of returning the cached entity.
     * @param jobId job id
     * @return JobDto
     */
    @Transactional(readOnly = true)
    public JobDto detachAndGetJobById(final String jobId) {
        final var jobEntity = findJobEntity(jobId);
        entityManager.detach(jobEntity);
        return jobMapper.apply(findJobEntity(jobId));
    }

    /**
     * Get all jobs with pagination.
     * Note: Jobs in state SCHEDULED are internal and will not be returned.
     * @param offset page offset, defaults to 0 if null or not an int
     * @param limit page limit, defaults to 100 if null or not an int
     * @param sort page sort by attribute and direction
     * @param filters filters
     *
     * @return JobListDto
     */
    @Transactional(readOnly = true)
    public JobListDto getJobs(final String offset, final String limit, final String sort, final String filters) {
        final var pageLimit = NumberUtils.toInt(limit, defaultLimit);
        final var pageOffset = NumberUtils.toInt(offset, OffsetPageRequest.DEFAULT_OFFSET);
        final var pageRequest = new OffsetPageRequest(pageOffset, pageLimit, new JobSortMapper().apply(sort));


        final Page<JobEntity> jobs;
        final Specification<JobEntity> notScheduledJobsSpec =
                (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get(JobProperty.STATUS.getMappedName()), SCHEDULED_STATUS);
        if (StringUtils.isEmpty(filters)) {
            jobs = jobRepository.findAll(notScheduledJobsSpec, pageRequest);
        } else {
            final Specification<JobEntity> specification = notScheduledJobsSpec.and(
                    new SpecificationMapper<JobEntity>(new JobProperties()).apply(filters));
            jobs = jobRepository.findAll(specification, pageRequest);
        }

        final List<JobSummaryDto> jobSummaryDtos = StreamSupport.stream(jobs.spliterator(), false)
                .map(jobEntity -> new JobSummaryMapper().apply(jobEntity))
                .toList();

        return new JobListDto().items(jobSummaryDtos).totalCount((int) jobs.getTotalElements());
    }

    /**
     * Delete job by id.
     * The job specification will also be deleted if the job is not associated with a job schedule or
     * the associated job schedule no longer exists and it is the last associated job.
     * Without the force option, only completed jobs can be deleted. The force option
     * can be used to delete running jobs.
     * A running job cannot be force deleted if associated with an enabled job schedule. The job schedule
     * needs to disabled first.
     * @param jobId id of job to be deleted
     * @param force if true then delete the job without any validation
     * @throws DRServiceException when job discovery/reconciliation in progress
     */
    public void deleteJobById(final String jobId, final boolean force) {
        final JobEntity jobEntity = findJobEntity(jobId);
        final Optional<JobScheduleEntity> jobScheduleEntity = Optional.ofNullable(jobEntity.getJobScheduleId())
                .map(id -> jobScheduleRepository.findById(jobEntity.getJobScheduleId()).orElse(null));
        new DeleteJobValidator(force).accept(jobEntity, jobScheduleEntity.orElse(null));
        jobRepository.delete(jobEntity);
        if (jobEntity.getJobScheduleId() == null ||
                (!jobScheduleEntity.isPresent() && jobRepository.countByJobScheduleId(jobEntity.getJobScheduleId()) == 0)) {
            jobSpecificationRepository.delete(jobEntity.getJobSpecification());
        }
    }

    /**
     * Delete jobs matching the specified filter.
     * <p>If force flag is false then jobs which are inprogress (DISCOVERY_INPROGRESS, RECONCILE_INPROGRESS) will not be deleted. The delete operation
     * will succeed, deleting all jobs matched by the filter excluding any inprogress jobs.</p>
     * <p>If force flag is true then inprogress jobs will also be deleted, except inprogress scheduled jobs. If the filter matches
     * one or more inprogress schedule jobs, then the delete operation will fail with error containing the ids of the jobs.</p>
     * @param filters job filters
     * @param force force flag to allow deletion of inprogress jobs
     */
    @Transactional
    public DeleteJobsResponseDto deleteJobs(final String filters, final boolean force) {
        if (StringUtils.isEmpty(filters)) {
            throw new DRServiceException(ErrorCode.INVALID_FILTER_PARAM, "Cannot be empty");
        }

        final Specification<JobEntity> filtersSpec = new SpecificationMapper<JobEntity>(new JobProperties()).apply(filters);

        final Specification<JobEntity> undeleteableJobStatesSpec;
        if (force) {
            final Specification<JobEntity> inProgressScheduledJobsSpec = filtersSpec
                    .and((root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(JobProperty.JOB_SCHEDULE_ID.getMappedName())))
                    .and((root, query, criteriaBuilder) -> root.get(JobProperty.STATUS.getMappedName()).in(
                            DISCOVERY_INPROGRESS.toString(), RECONCILE_INPROGRESS.toString()));
            final List<JobEntity> inProgressScheduledJobs = jobRepository.findAll(inProgressScheduledJobsSpec);
            if (!inProgressScheduledJobs.isEmpty()) {
                throw new DRServiceException(ErrorCode.CANNOT_FORCE_DELETE_INPROGRESS_SCHEDULED_JOBS,
                        inProgressScheduledJobs.stream().map(JobEntity::getId).toList().toString());
            }
            undeleteableJobStatesSpec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.notEqual(root.get(JobProperty.STATUS.getMappedName()), SCHEDULED_STATUS);
        } else {
            undeleteableJobStatesSpec = (root, query, criteriaBuilder) -> criteriaBuilder.not(root.get(JobProperty.STATUS.getMappedName()).in(
                    SCHEDULED_STATUS, DISCOVERY_INPROGRESS.toString(), RECONCILE_INPROGRESS.toString()));
        }
        final Specification<JobEntity> specification = filtersSpec.and(undeleteableJobStatesSpec);
        final long deletedJobsCount = jobRepository.delete(specification);
        return new DeleteJobsResponseDto().deleted(deletedJobsCount);
    }

    /**
     * Delete jobs older than given maxAge.
     * Jobs with status DISCOVERY_INPROGRESS and RECONCILE_INPROGRESS will not be deleted.
     *
     * @param timeUnit
     *         time unit i.e. days
     * @param maxAge
     *         jobs older than maxAge will be deleted
     * @return number of jobs deleted
     */
    @Transactional
    public Long deleteOldJobs(final ChronoUnit timeUnit, final int maxAge) {
        final LocalDateTime cutOffDate = LocalDateTime.now().minus(maxAge, timeUnit);
        final List<String> jobStatesExcludingInProgress = Arrays.stream(values())
                .filter(state -> !Arrays.asList(DISCOVERY_INPROGRESS, RECONCILE_INPROGRESS).contains(state))
                .map(JobSummaryDto.StatusEnum::toString)
                .toList();
        return jobRepository.deleteByStartDateBeforeAndJobStatusIn(Timestamp.valueOf(cutOffDate), jobStatesExcludingInProgress);
    }

    /**
     * Sets a job to an InProgress state (DISCOVERY_INPROGRESS, RECONCILE_INPROGRESS).
     * The start date will be set if the state is DISCOVERY_INPROGRESS.
     * The executor corresponding to the host executing the job is also set.
     * @param jobId job id
     * @param status job status
     * @return JobEntity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity setJobInProgress(final long jobId, final String status) {
        final var jobEntity = findJobEntity(String.valueOf(jobId));
        jobEntity.setJobStatus(status);
        jobEntity.setExecutor(podName);
        if (DISCOVERY_INPROGRESS.toString().equals(status)) {
            jobEntity.setStartDate(Date.from(Instant.now()));
        }
        return jobRepository.save(jobEntity);
    }

    /**
     * Update a JobEntity.
     * @param jobEntity job entity
     * @return JobEntity
     */
    public JobEntity updateJobEntity(final JobEntity jobEntity) {
        return jobRepository.save(jobEntity);
    }

    /**
     * Update the job state after discovery completed.
     * @param jobId job id
     * @param discoveredCount discovered object count
     * @return JobEntity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity discoveryCompleted(final long jobId, final int discoveredCount) {
        final var jobEntity = findJobEntity(String.valueOf(jobId));
        final var jobStatus = discoveredCount == 0 ? COMPLETED : DISCOVERED;
        jobEntity.setJobStatus(jobStatus.name());
        if (jobStatus.equals(COMPLETED)) {
            jobEntity.setCompletedDate(Date.from(Instant.now()));
        }
        jobEntity.setDiscoveredObjectsCount(discoveredCount);
        jobEntity.unlock();
        return jobRepository.save(jobEntity);
    }

    /**
     * Update the job state after reconcile completed or failed.
     * @param jobId job id
     * @param statusCounts discovered object states and counts
     * @return JobEntity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity reconcileCompleted(final long jobId, final DiscoveredObjectStatusCounts statusCounts) {
        final var jobEntity = findJobEntity(String.valueOf(jobId));
        final String jobStatus = JobStatusEvaluator.evaluate(statusCounts);
        if (COMPLETED.name().equals(jobStatus)) {
            jobEntity.setCompletedDate(Date.from(Instant.now()));
        }
        jobEntity.setJobStatus(jobStatus);
        jobEntity.setReconciledObjectsCount(statusCounts.getReconciledCount());
        jobEntity.setReconciledObjectsErrorCount(statusCounts.getReconcileFailedCount());
        jobEntity.unlock();
        if (shouldRaiseAlarm.test(jobEntity)) {
            alarmHandlerClient.raiseAlarm(
                    FaultName.JOB_FAILED, new JobAlarmDescription(jobEntity), new JobAlarmResource(jobEntity));
        }
        return jobRepository.save(jobEntity);
    }

    /**
     * Update the job state with an associated error message.
     * @param jobId job id
     * @param errorMsg error message
     * @return JobEntity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobEntity discoveryFailed(final long jobId, final Optional<String> errorMsg) {
        final var jobEntity = findJobEntity(String.valueOf(jobId));
        jobEntity.setJobStatus(DISCOVERY_FAILED.toString());
        errorMsg.ifPresent(jobEntity::setErrorMessage);
        if (shouldRaiseAlarm.test(jobEntity)) {
            alarmHandlerClient.raiseAlarm(
                    FaultName.JOB_FAILED, new JobAlarmDescription(jobEntity), new JobAlarmResource(jobEntity));
        }
        jobEntity.unlock();
        return jobRepository.save(jobEntity);
    }


    /**
     * Find JobEntity.
     * @param jobId job id
     * @return JobEntity
     */
    public JobEntity findJobEntity(final String jobId) {
        return jobRepository.findById(Long.valueOf(jobId))
                .orElseThrow(() -> new DRServiceException(JOB_NOT_FOUND, jobId));
    }

    /**
     * Finds and locks executable jobs in a new transaction.
     * @param limit the max number of jobs to be returned
     * @return JobEntity list
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<JobEntity> findAndLockExecutableJobs(final int limit) {
        final var pageable = PageRequest.of(0, limit, Sort.by("modifiedDate").ascending());
        final List<JobEntity> jobEntities = jobRepository.findExecutableJobs(pageable, LocalDateTime.now());
        jobEntities.stream().forEach(JobEntity::lock);
        return jobEntities;
    }

    /**
     * Unlocks the specified job by updating the locked and lockTime properties.
     * Executes in a new tx.
     * @param jobId job id
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockJob(final long jobId) {
        LOGGER.debug("UnLocking job {}", jobId);
        final var jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found"));
        jobEntity.unlock();
        jobRepository.save(jobEntity);
    }

    /**
     * Find all running scheduled jobs. Returns at most one job per schedule as there
     * should never be multiple running jobs for the same schedule. If multiple jobs
     * are found for a schedule, they will be ignored.
     * @return JobEntity collection
     */
    public Collection<JobEntity> findRunningScheduledJobs() {
        final Map<Long, JobEntity> runningJobsByScheduleId = new HashMap<>();
        jobRepository.findByJobScheduleIdIsNotNullAndJobStatusIn(Arrays.asList("DISCOVERY_INPROGRESS", "RECONCILE_INPROGRESS")).forEach(j -> {
            if (runningJobsByScheduleId.containsKey(j.getJobScheduleId())) {
                LOGGER.warn("Found more than 1 running job {} for schedule {}", j.getId(), j.getJobScheduleId());
            } else {
                runningJobsByScheduleId.put(j.getJobScheduleId(), j);
            }
        });
        return runningJobsByScheduleId.values();
    }

    private JobEntity createJobEntity(final ExecuteJobDto executeJobDto, final Long messageSubscriptionId) {
        createJobValidator.accept(executeJobDto);
        final var jobEntity = new JobEntityMapper(messageSubscriptionId).apply(executeJobDto);
        applicationRepository.findById(Long.valueOf(executeJobDto.getApplicationId()))
                .map(app -> app.findJob(executeJobDto.getApplicationJobName()))
                .ifPresent(jobEntity::setApiPropertyNames);
        return jobEntity;
    }

}