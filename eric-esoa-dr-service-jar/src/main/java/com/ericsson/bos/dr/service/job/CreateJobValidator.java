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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.ericsson.bos.dr.jpa.model.ApplicationEntity;
import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.service.FeaturePackService;
import com.ericsson.bos.dr.service.discovery.functions.InputsValidator;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.utils.JobNameGenerator;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationInputsDto;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationJobDto;
import com.ericsson.bos.dr.web.v1.api.model.ExecuteJobDto;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validate a job request <code>ExecuteJobDto</code>.
 * <ul>
 *     <li>Check at least one of featurepack id or name is set.</li>
 *     <li>Check at least one of application id or name is set.</li>
 *     <li>Check job name is set else autogenerate the name.</li>
 *     <li>Check feature pack exists. If found the id or name in the <code>ExecuteJobDto </code>are updated if missing.</li>
 *     <li>Check application exists. If found the id or name in the <code>ExecuteJobDto </code>are updated if missing.</li>
 *     <li>Check the application job exists.</li>
 *     <li>Check mandatory discovery inputs are provided.</li>
 *     <li>Check mandatory reconcile inputs are provided if autoreconcile is true.</li>
 * </ul>
 */
@Component
public class CreateJobValidator implements Consumer<ExecuteJobDto> {

    @Autowired
    private FeaturePackService featurePackService;
    
    @Autowired
    private InputsValidator inputsValidator;

    @Override
    public void accept(final ExecuteJobDto executeJobDto) {
        validateFeaturePackAndApplicationProperties(executeJobDto);
        validateJobNameSetElseAutoGenerate(executeJobDto);
        final FeaturePackEntity featurePackEntity = validateFeaturePackExists(executeJobDto);
        final ApplicationEntity applicationEntity = validateApplicationExists(executeJobDto, featurePackEntity);
        final ApplicationConfigurationJobDto appJobDto = validateApplicationJobExists(executeJobDto, applicationEntity);
        validateDiscoveryAndReconcileInputs(executeJobDto, appJobDto);
    }

    private void validateFeaturePackAndApplicationProperties(final ExecuteJobDto executeJobDto) {
        if ((StringUtils.isEmpty(executeJobDto.getApplicationId()) && StringUtils.isEmpty(executeJobDto.getApplicationName())) ||
                (StringUtils.isEmpty(executeJobDto.getFeaturePackName()) && StringUtils.isEmpty(executeJobDto.getFeaturePackId()))) {
            throw new DRServiceException(ErrorCode.ID_OR_NAME_NOT_PROVIDED);
        }
    }

    private void validateJobNameSetElseAutoGenerate(final ExecuteJobDto executeJobDto) {
        if (executeJobDto.getName() == null) {
            executeJobDto.setName(new JobNameGenerator().apply(executeJobDto));
        }
    }

    private FeaturePackEntity validateFeaturePackExists(final ExecuteJobDto executeJobDto) {
        final FeaturePackEntity featurePackEntity;
        if (executeJobDto.getFeaturePackId() != null) {
            featurePackEntity = featurePackService.findFeaturePackById(executeJobDto.getFeaturePackId());
            executeJobDto.setFeaturePackName(featurePackEntity.getName());
        } else {
            featurePackEntity = featurePackService.findFeaturePackByName(executeJobDto.getFeaturePackName());
            executeJobDto.setFeaturePackId(String.valueOf(featurePackEntity.getId()));
        }
        return featurePackEntity;
    }

    private ApplicationEntity validateApplicationExists(final ExecuteJobDto executeJobDto, final FeaturePackEntity featurePackEntity) {
        final ApplicationEntity applicationEntity;
        if (executeJobDto.getApplicationId() != null) {
            applicationEntity = getApplication(featurePackEntity, app -> app.getId().equals(
                    Long.valueOf(executeJobDto.getApplicationId())), executeJobDto.getApplicationId());
            executeJobDto.setApplicationName(applicationEntity.getName());
        } else {
            applicationEntity = getApplication(featurePackEntity, app -> app.getName().equals(
                    executeJobDto.getApplicationName()), executeJobDto.getApplicationName());
            executeJobDto.setApplicationId(String.valueOf(applicationEntity.getId()));
        }
        return applicationEntity;
    }

    private void validateDiscoveryAndReconcileInputs(final ExecuteJobDto executeJobDto, final ApplicationConfigurationJobDto appJobDto) {
        if (MapUtils.isNotEmpty(executeJobDto.getInputs())) {
            final List<ApplicationConfigurationInputsDto> discoveryInputs = Optional.ofNullable(
                    appJobDto.getDiscover().getInputs()).orElse(Collections.emptyList());
            inputsValidator.validate(discoveryInputs, executeJobDto.getInputs());
            if (executeJobDto.getExecutionOptions() != null && executeJobDto.getExecutionOptions().getAutoReconcile()) {
                final List<ApplicationConfigurationInputsDto> reconcileInputs = Optional.ofNullable(
                        appJobDto.getReconcile().getInputs()).orElse(Collections.emptyList());
                inputsValidator.validate(reconcileInputs, executeJobDto.getInputs());
            }
        }
    }

    private ApplicationConfigurationJobDto validateApplicationJobExists(final ExecuteJobDto executeJobDto,
                                                                        final ApplicationEntity applicationEntity) {
        final ApplicationConfigurationJobDto appJobDto = applicationEntity.findJob(executeJobDto.getApplicationJobName());
        if (appJobDto == null) {
            throw new DRServiceException(ErrorCode.JOB_NOT_FOUND, executeJobDto.getApplicationJobName());
        }
        return appJobDto;
    }

    private ApplicationEntity getApplication(final FeaturePackEntity featurePackEntity,
                                             final Predicate<ApplicationEntity> predicate, final String appIdOrName) {
        return featurePackEntity.getApplications().stream()
                .filter(predicate)
                .findAny()
                .orElseThrow(() -> new DRServiceException(
                        APP_NOT_FOUND, appIdOrName, featurePackEntity.getName()));
    }
}