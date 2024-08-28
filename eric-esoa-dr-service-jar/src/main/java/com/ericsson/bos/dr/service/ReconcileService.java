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

import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.DISCOVERED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.PARTIALLY_RECONCILED;
import static com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto.StatusEnum.RECONCILE_FAILED;

import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.model.JobEntity;
import com.ericsson.bos.dr.service.discovery.functions.InputsValidator;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.reconcile.ReconcileContext;
import com.ericsson.bos.dr.service.reconcile.ReconcileFlow;
import com.ericsson.bos.dr.service.utils.MapUtils;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteReconcileDtoObjectsInner;
import com.ericsson.bos.dr.web.v1.api.model.JobSummaryDto;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reconcile service.
 */
@Component
public class ReconcileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconcileService.class);

    @Autowired
    private ReconcileFlow reconcileFlow;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private InputsValidator inputsValidator;

    /**
     * Start reconcile.
     *
     * @param jobId            discovery job id
     * @param reconcileDetails reconcile request details
     */
    public void requestReconcile(String jobId, ExecuteReconcileDto reconcileDetails) {
        final var jobEntity = jobService.findJobEntity(jobId);
        final String jobStatus = jobEntity.getJobStatus();
        LOGGER.debug("Reconcile requests for job {}, status={}, locked={}", jobId, jobStatus, jobEntity.isLocked());
        if (!DISCOVERED.toString().equals(jobStatus) && !PARTIALLY_RECONCILED.toString().equals(jobStatus)
            && !RECONCILE_FAILED.toString().equals(jobStatus)) {
            throw new DRServiceException(ErrorCode.INVALID_STATE_FOR_RECONCILE, jobId, jobStatus);
        }
        final var jobConf = getJobDefinition(jobEntity);
        validateInputs(jobEntity, reconcileDetails, jobConf);

        jobEntity.setJobStatus(JobSummaryDto.StatusEnum.RECONCILE_REQUESTED.toString());
        jobEntity.setReconcileRequest(reconcileDetails);
        jobService.updateJobEntity(jobEntity);
    }

    /**
     * Execute reconcile and return when complete.
     *
     * @param jobId            discovery job id
     * @param reconcileDetails reconcile request details
     */
    public void executeReconcile(String jobId, ExecuteReconcileDto reconcileDetails) {
        final var jobEntity = jobService.findJobEntity(jobId);
        LOGGER.debug("Execute reconcile for job {}, status={}, locked={}", jobId, jobEntity.getJobStatus(), jobEntity.isLocked());
        final var jobConf = getJobDefinition(jobEntity);
        validateInputs(jobEntity, reconcileDetails, jobConf);

        if (CollectionUtils.isEmpty(reconcileDetails.getObjects())) {
            LOGGER.info("Start reconcile for all objects: jobId={}, filters={}", jobId, reconcileDetails.getFilters());
            // update the request with all discovered object ids if none set
            discoveryObjectRepository.getIdsByJobIdAndStatusNot(Long.valueOf(jobId), DiscoveredObjectDto.StatusEnum.RECONCILED.toString()).stream()
                    .forEach(id -> reconcileDetails.addObjectsItem(
                            new ExecuteReconcileDtoObjectsInner().objectId(id.toString())));
        } else {
            LOGGER.info("Start reconcile for specified objects: jobId={}, filters={}, objects={}", jobId,
                    reconcileDetails.getFilters(), reconcileDetails.getObjects());
        }

        final var reconcileContext =
                ReconcileContext.initialize(Long.valueOf(jobId), jobEntity, reconcileDetails, jobConf);
        reconcileFlow.execute(reconcileContext).join();
    }

    private ApplicationConfigurationJobDto getJobDefinition(final JobEntity jobEntity) {
        final var applicationEntity = applicationService.findApplicationEntity(jobEntity.getFeaturePackId().toString(),
                jobEntity.getApplicationId().toString());
        return Optional.ofNullable(applicationEntity.findJob(jobEntity.getApplicationJobName()))
                .orElseThrow(() -> new IllegalStateException("Job configuration not found"));
    }

    private void validateInputs(final JobEntity jobEntity, final ExecuteReconcileDto reconcileDetails,
                                final ApplicationConfigurationJobDto jobConf) {
        final Map<String, Object> inputs = MapUtils.merge(reconcileDetails.getInputs(), jobEntity.getInputs());
        if (jobConf.getReconcile().getInputs() != null) {
            inputsValidator.validate(jobConf.getReconcile().getInputs(), inputs);
        }
    }
}