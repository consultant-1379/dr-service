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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.APP_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND;
import java.util.List;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.ApplicationRepository;
import com.ericsson.bos.dr.jpa.FeaturePackRepository;
import com.ericsson.bos.dr.jpa.model.ApplicationEntity;
import com.ericsson.bos.dr.model.mappers.ApplicationSummaryMapper;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Managed Application configurations.
 */
@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private FeaturePackRepository featurePackRepository;


    /**
     * Get all application configurations in a feature pack.
     *
     * @param featurePackId feature pack id
     * @return ConfigurationListDto
     */
    @Transactional(readOnly = true)
    public ConfigurationListDto getApplications(final String featurePackId) {
        final List<ConfigurationSummaryDto> apps = applicationRepository.findByFeaturePackId(Long.valueOf(featurePackId)).stream()
                .map(ae -> new ApplicationSummaryMapper().apply(ae))
                .collect(Collectors.toList());
        return new ConfigurationListDto().items(apps).totalCount(apps.size());
    }

    /**
     * Get specific application in a feature pack.
     *
     * @param featurePackId feature pack id
     * @param appId         application id
     * @return ApplicationConfigurationDto
     */
    @Transactional(readOnly = true)
    public ApplicationConfigurationDto getApplication(final String featurePackId, final String appId) {
        final var appEntity = findApplicationEntity(validateFeaturePackId(featurePackId), appId);
        final var appConfig = appEntity.getConfig();
        appConfig.setId(appEntity.getId().toString());
        return appConfig;
    }

    /**
     * Find Application Entity in a feature pack.
     * @param featurePackId feature pack id
     * @param appId application id
     * @return ApplicationEntity
     */
    public ApplicationEntity findApplicationEntity(final String featurePackId, final String appId) {
        return applicationRepository
                .findByIdAndFeaturePackId(Long.valueOf(appId), Long.valueOf(featurePackId))
                .orElseThrow(() -> new DRServiceException(APP_NOT_FOUND, appId, featurePackId));
    }

    private String validateFeaturePackId(final String featurePackId) {
        final var featurePackEntity =  featurePackRepository.findById(Long.valueOf(featurePackId))
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackId));
        return String.valueOf(featurePackEntity.getId());
    }
}