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
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERY_INPROGRESS;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_INPROGRESS;
import static org.apache.commons.lang3.math.NumberUtils.toInt;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import com.ericsson.bos.dr.jpa.FeaturePackRepository;
import com.ericsson.bos.dr.jpa.JobRepository;
import com.ericsson.bos.dr.jpa.OffsetPageRequest;
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.model.mappers.FeaturePackEntityMapper;
import com.ericsson.bos.dr.model.mappers.FeaturePackMapper;
import com.ericsson.bos.dr.model.mappers.FeaturePackProperties;
import com.ericsson.bos.dr.model.mappers.FeaturePackSortMapper;
import com.ericsson.bos.dr.model.mappers.FeaturePackSummaryMapper;
import com.ericsson.bos.dr.model.mappers.SpecificationMapper;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.featurepacks.FeaturePackArchiver;
import com.ericsson.bos.dr.service.featurepacks.FeaturePackDownloadResource;
import com.ericsson.bos.dr.service.featurepacks.FeaturePackExtractor;
import com.ericsson.bos.dr.service.featurepacks.FeaturePackValidator;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackListDto;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackSummaryDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Manage Feature Packs.
 */
@Service
public class FeaturePackService {

    @Value("${service.pagination.default_limit}")
    private int defaultLimit;

    @Autowired
    private FeaturePackExtractor featurePackExtractor;

    @Autowired
    private FeaturePackArchiver featurePackArchiver;

    @Autowired
    private FeaturePackRepository featurePackRepository;

    @Autowired
    private FeaturePackValidator featurePackValidator;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    @Lazy
    private JobScheduleService jobScheduleService;

    /**
     * Create new feature pack from archive.
     *
     * @param name        feature pack name
     * @param description feature pack description
     * @param file        feature pack archive
     * @return FeaturePackDto
     */
    public FeaturePackDto createFeaturePack(final String name, final String description, final MultipartFile file) {
        featurePackRepository.findByName(name).ifPresent(
                fp -> { throw new DRServiceException(ErrorCode.FP_ALREADY_EXISTS, name); });
        final var featurePackEntity = saveFeaturePack(name, description, file);
        return new FeaturePackMapper().apply(featurePackEntity);
    }

    /**
     * Delete a feature pack identified by its id.
     *
     * @param featurePackId feature pack id
     */
    @Transactional
    public void deleteFeaturePack(final String featurePackId) {
        validateNoJobsOngoing(featurePackId);
        featurePackRepository.deleteById(findFeaturePackById(featurePackId).getId());
        jobScheduleService.deleteFeaturePackJobSchedules(featurePackId);
    }

    /**
     * Get all feature packs with pagination.
     *
     * @param offset  page offset, defaults to 0 if null or not an int
     * @param limit   page limit, defaults to 100 if null or not an int
     * @param sort    page sort by attribute and direction
     * @param filters filters
     * @return FeaturePackListDto
     */
    @Transactional(readOnly = true)
    public FeaturePackListDto getFeaturePacks(String offset, String limit, String sort, String filters) {
        final var pageRequest = new OffsetPageRequest(toInt(offset, DEFAULT_OFFSET), toInt(limit, defaultLimit),
                new FeaturePackSortMapper().apply(sort));

        final Page<FeaturePackEntity> featurePacks;
        if (StringUtils.isEmpty(filters)) {
            featurePacks = featurePackRepository.findAll(pageRequest);
        } else {
            final Specification<FeaturePackEntity> specification =
                    new SpecificationMapper<FeaturePackEntity>(new FeaturePackProperties()).apply(filters);
            featurePacks = featurePackRepository.findAll(specification, pageRequest);
        }

        final List<FeaturePackSummaryDto> featurePackSummaryDtos = StreamSupport.stream(featurePacks.spliterator(), false)
                .map(fpe -> new FeaturePackSummaryMapper().apply(fpe))
                .toList();

        return new FeaturePackListDto().items(featurePackSummaryDtos).totalCount((int) featurePacks.getTotalElements());
    }

    /**
     * Get a feature pack identified by its id.
     *
     * @param featurePackId feature pack id
     * @return FeaturePackDto
     */
    @Transactional(readOnly = true)
    public FeaturePackDto getFeaturePack(final String featurePackId) {
        return new FeaturePackMapper().apply(findFeaturePackById(featurePackId));
    }

    /**
     * Download the feature pack as a zip archive. The archive contains the original files that
     * were in the feature pack.
     *
     * @param featurePackId feature pack id
     * @return FeaturePackDownloadResource
     */
    @Transactional(readOnly = true)
    public FeaturePackDownloadResource downloadFeaturePack(final String featurePackId) {
        final var featurePackEntity = findFeaturePackById(featurePackId);
        return new FeaturePackDownloadResource(featurePackEntity.getName(), featurePackArchiver.zip(featurePackEntity));
    }

    /**
     * Find a feature pack by id.
     *
     * @param featurePackId feature pack id
     * @return FeaturePackEntity
     */
    public FeaturePackEntity findFeaturePackById(final String featurePackId) {
        return featurePackRepository.findById(Long.valueOf(featurePackId))
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackId));
    }
    /**
     * Find a feature pack by name.
     *
     * @param featurePackName feature pack name
     * @return FeaturePackEntity
     */
    public FeaturePackEntity findFeaturePackByName(final String featurePackName) {
        return featurePackRepository.findByName(featurePackName)
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackName));
    }

    /**
     * Replace a feature pack. This deletes the existing feature pack and replaces it with
     * the new feature pack.
     *
     * @param featurePackId feature pack id
     * @param description   feature pack description
     * @param file          feature pack archive
     * @return FeaturePackDto
     */
    @Transactional
    public FeaturePackDto replaceFeaturePack(final String featurePackId, final String description, final MultipartFile file) {
        validateNoJobsOngoing(featurePackId);
        final var existingFeaturePackEntity = findFeaturePackById(featurePackId);
        featurePackRepository.delete(existingFeaturePackEntity);
        jobScheduleService.deleteFeaturePackJobSchedules(featurePackId);
        featurePackRepository.flush();
        final var updatedFeaturePackEntity =
                saveFeaturePack(existingFeaturePackEntity.getName(), description, file);
        return new FeaturePackMapper().apply(updatedFeaturePackEntity);
    }

    private FeaturePackEntity saveFeaturePack(final String featurePackName, final String description,
            final MultipartFile file) {
        final var configFiles = featurePackExtractor.unpack(file);
        featurePackValidator.validate(featurePackName, configFiles);
        final var featurePackEntity =
                new FeaturePackEntityMapper(featurePackName, description).apply(configFiles);
        return featurePackRepository.save(featurePackEntity);
    }

    private void validateNoJobsOngoing(final String featurePackId) {
        final List<String> inProgressJobs = jobRepository.findByJobSpecificationFeaturePackIdAndJobStatusIn(Long.valueOf(featurePackId),
                        Arrays.asList(DISCOVERY_INPROGRESS.toString(), RECONCILE_INPROGRESS.toString()))
                .stream().map(job -> job.getId().toString()).toList();
        if (!inProgressJobs.isEmpty()) {
            throw new DRServiceException(ErrorCode.FP_JOB_INPROGRESS, inProgressJobs.toString(), featurePackId);
        }
    }
}
