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

import static com.ericsson.bos.dr.jpa.DiscoveryObjectRepository.jobIdEquals;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.JOB_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.OBJECT_NOT_FOUND;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.JobRepository;
import com.ericsson.bos.dr.jpa.OffsetPageRequest;
import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.model.mappers.DiscoveredObjectProperties;
import com.ericsson.bos.dr.model.mappers.DiscoveredObjectSortMapper;
import com.ericsson.bos.dr.model.mappers.DiscoveryObjectMapper;
import com.ericsson.bos.dr.model.mappers.ExecuteJobDtoMapper;
import com.ericsson.bos.dr.model.mappers.SpecificationMapper;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.service.discovery.DiscoveryFlow;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectListDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Discovery Service.
 */
@Component
public class DiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryService.class);

    @Value("${service.pagination.default_limit}")
    private int defaultLimit;

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private DiscoveryFlow discoveryFlow;

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    @Autowired
    private JobRepository jobRepository;

    /**
     * Start a discovery job.
     * @param discoveryJob the discovery job
     * @return job id
     */
    public String startDiscovery(final ExecuteJobDto discoveryJob) {
        final var jobEntity = jobService.createJob(discoveryJob);
        return jobEntity.getId().toString();
    }

    /**
     * Start a discovery job. Used when discovery was triggered by a message.
     * @param discoveryJob the discovery job
     * @param messageSubscriptionId message subscription id
     * @return job id
     */
    public String startDiscovery(final ExecuteJobDto discoveryJob, final long messageSubscriptionId) {
        final var jobEntity = jobService.createJob(discoveryJob, messageSubscriptionId);
        return jobEntity.getId().toString();
    }

    /**
     * Start a Discovery job using the properties from an existing job.
     *
     * @param originalJobId
     *         job to duplicate properties from
     * @return job id
     */
    public String startDuplicateDiscovery(final String originalJobId) {
        final JobEntity originalJobEntity = jobRepository.findById(Long.parseLong(originalJobId))
                .orElseThrow(() -> new DRServiceException(JOB_NOT_FOUND, originalJobId));
        final var jobEntity = jobService.createJob(new ExecuteJobDtoMapper().apply(originalJobEntity));
        return jobEntity.getId().toString();
    }

    /**
     * Executes the discovery job and returns when complete.
     * @param jobEntity job entity
     */
    public void executeDiscovery(final JobEntity jobEntity) {
        startDiscovery(jobEntity).join();
    }

    private CompletableFuture<Void> startDiscovery(final JobEntity jobEntity) {
        final var appEntity = applicationService.findApplicationEntity(jobEntity.getFeaturePackId().toString(),
                jobEntity.getApplicationId().toString());
        final var discoveryContext = DiscoveryContext.initialize(appEntity, jobEntity);
        LOGGER.info("Starting discovery flow: fpName={}, jobName={}, jobId={}",
                discoveryContext.getFeaturePackName(), jobEntity.getName(), jobEntity.getId());
        return discoveryFlow.execute(discoveryContext);
    }

    /**
     * Get paged discovered objects.
     * @param jobId job id
     * @param offset optional offset, uses default if not set
     * @param limit optional limit, uses default if not set
     * @param sort optional sort, uses default if not set
     * @param filters filters
     * @return DiscoveredObjectListDto
     */
    public DiscoveredObjectListDto getDiscoveredObjects(final String jobId, final String offset, final String limit,
                                                          final String sort, final String filters) {
        final var pageLimit = NumberUtils.toInt(limit, defaultLimit);
        final var pageOffset = NumberUtils.toInt(offset, OffsetPageRequest.DEFAULT_OFFSET);
        final Pageable pageRequest = new OffsetPageRequest(pageOffset, pageLimit, new DiscoveredObjectSortMapper().apply(sort));

        final Page<DiscoveryObjectEntity> page;
        if (StringUtils.isEmpty(filters)) {
            page = discoveryObjectRepository.findAll(jobIdEquals(Long.parseLong(jobId)), pageRequest);
        } else {
            final Specification<DiscoveryObjectEntity> specification = jobIdEquals(Long.parseLong(jobId)).and(
                    new SpecificationMapper<>(new DiscoveredObjectProperties()).apply(filters));
            page = discoveryObjectRepository.findAll(specification, pageRequest);
        }

        final List<String> apiPropertyNames = getApiPropertyNames(jobId);
        final List<DiscoveredObjectDto> items = page.get()
                .map(entity -> new DiscoveryObjectMapper(apiPropertyNames).apply(entity))
                .collect(Collectors.toList());
        return new DiscoveredObjectListDto().items(items).totalCount((int) (page.getTotalElements()));
    }

    private List<String> getApiPropertyNames(final String jobId) {
        final Optional<JobEntity> jobEntity = jobRepository.findById(Long.parseLong(jobId));
        return jobEntity.map(JobEntity::getApiPropertyNames)
                .orElse(new ArrayList<>());
    }

    /**
     * Get all discovered objects for a job.
     * @param jobId job id
     * @return DiscoveredObjectListDto
     */
    public DiscoveredObjectListDto getAllDiscoveredObject(final String jobId) {
        final List<String> apiPropertyNames = getApiPropertyNames(jobId);
        final List<DiscoveredObjectDto> objects = discoveryObjectRepository.findAll(jobIdEquals(Long.parseLong(jobId))).stream()
                .map(entity -> new DiscoveryObjectMapper(apiPropertyNames).apply(entity))
                .collect(Collectors.toList());
        return new DiscoveredObjectListDto().items(objects).totalCount((objects.size()));
    }

    /**
     * Find DiscoveryObjectEntity by id.
     * @param id id
     * @return DiscoveryObjectEntity
     */
    public DiscoveryObjectEntity findDiscoveryObjectEntityById(Long id) {
        return discoveryObjectRepository.findById(id)
                .orElseThrow(() -> new DRServiceException(OBJECT_NOT_FOUND, id.toString()));
    }
}