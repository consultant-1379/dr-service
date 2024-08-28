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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.BAD_REQUEST_PARAM;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONFIG_EXISTS;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONFIG_NAME_MISMATCH;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.CONFIG_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.FP_NOT_FOUND;
import static com.ericsson.bos.dr.service.exceptions.ErrorCode.READ_ONLY_ACCESS;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.bos.dr.jpa.FeaturePackRepository;
import com.ericsson.bos.dr.jpa.InputConfigurationsRepository;
import com.ericsson.bos.dr.jpa.model.InputConfigurationEntity;
import com.ericsson.bos.dr.model.mappers.InputConfigurationEntityMapper;
import com.ericsson.bos.dr.model.mappers.InputConfigurationMapper;
import com.ericsson.bos.dr.model.mappers.InputConfigurationSummaryMapper;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine;
import com.ericsson.bos.dr.service.utils.JSON;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationSummaryDto;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDtoAllOfInputs;

/**
 * Manage inputs.
 */
@Service
public class InputConfigurationsService {

    @Autowired
    private FeaturePackService featurePackService;

    @Autowired
    private InputConfigurationsRepository inputConfigurationsRepository;

    @Autowired
    private SubstitutionEngine substitutionEngine;

    @Autowired
    private FeaturePackRepository featurePackRepository;


    /**
     * Gat all input configurations in a feature pack.
     *
     * @param featurePackId
     *         feature pack id
     * @return ConfigurationListDto
     */
    public ConfigurationListDto getInputConfigurations(final String featurePackId) {
        final List<ConfigurationSummaryDto> inputConfigs = inputConfigurationsRepository.findByFeaturePackId(Long.parseLong(featurePackId))
                .stream()
                .map(inputEntity -> new InputConfigurationSummaryMapper().apply(inputEntity))
                .toList();
        return new ConfigurationListDto().items(inputConfigs).totalCount(inputConfigs.size());
    }

    /**
     * Get a specific input configuration in a feature pack.
     *
     * @param featurePackId
     *         feature pack id
     * @param configurationId
     *         input configuration id
     * @param evaluateFunctions
     *         evaluateFunctions flag
     * @return InputConfigurationDto
     */
    public InputConfigurationDto getInputConfiguration(final String featurePackId, final String configurationId, final boolean evaluateFunctions) {
        final var inputEntity = findInputEntity(featurePackId, configurationId);

        if (evaluateFunctions) {
            final InputConfigurationDto configurationDto = inputEntity.getConfig();
            for (final InputConfigurationDtoAllOfInputs input : configurationDto.getInputs()) {
                final Object pickListInput = input.getPickList();
                if (pickListInput instanceof final String stringInput) {
                    final String substitutionResult = StringUtils.trim(
                            substitutionEngine.render(stringInput, new HashMap<>(), Long.valueOf(featurePackId)));
                    input.setPickList(JSON.readObject(substitutionResult));
                }
            }
        }

        return new InputConfigurationMapper().apply(inputEntity);
    }

    private InputConfigurationEntity findInputEntity(final String featurePackId, final String configurationId) {
        final var featurePackEntity = featurePackRepository.findById(Long.valueOf(featurePackId))
                .orElseThrow(() -> new DRServiceException(FP_NOT_FOUND, featurePackId));
        return inputConfigurationsRepository
                .findByIdAndFeaturePackId(Long.parseLong(configurationId), featurePackEntity.getId())
                .orElseThrow(() -> new DRServiceException(CONFIG_NOT_FOUND, configurationId, featurePackId));
    }

    /**
     * Delete an input configuration in the feature pack.
     *
     * @param featurePackId
     *         feature pack id
     * @param configurationId
     *         input configuration id
     */
    public void deleteInputConfiguration(final String featurePackId, final String configurationId) {
        final var inputEntity = findInputEntity(featurePackId, configurationId);
        new ConfigValidator().writeEnabled(inputEntity);
        inputConfigurationsRepository.deleteById(inputEntity.getId());
    }

    /**
     * Create a new input configuration in the feature pack.
     *
     * @param featurePackId
     *         feature pack id
     * @param inputConfigurationDto
     *         input configuration id
     */
    public void createInputConfiguration(final String featurePackId, final InputConfigurationDto inputConfigurationDto) {
        final var featurePackEntity = featurePackService.findFeaturePackById(featurePackId);
        new ConfigValidator()
                .nameProvided(inputConfigurationDto)
                .nameUnique(inputConfigurationDto, featurePackId);
        inputConfigurationsRepository.save(new InputConfigurationEntityMapper(featurePackEntity).apply(inputConfigurationDto));
    }

    /**
     * Replace an input configuration in the feature pack.
     * The original configuration is deleted and replaced with the new configuration.
     *
     * @param featurePackId
     *         feature pack id
     * @param inputConfigurationId
     *         input configuration id
     * @param inputConfigurationDto
     *         InputConfigurationDto
     */
    @Transactional
    public void replaceInputConfiguration(final String featurePackId, final String inputConfigurationId,
                                          final InputConfigurationDto inputConfigurationDto) {
        final var existingInputEntity = findInputEntity(featurePackId, inputConfigurationId);
        new ConfigValidator()
                .writeEnabled(existingInputEntity)
                .nameProvided(inputConfigurationDto)
                .namesMatch(inputConfigurationDto, existingInputEntity);
        inputConfigurationsRepository.deleteById(existingInputEntity.getId());
        inputConfigurationsRepository.flush();
        inputConfigurationsRepository.save(new InputConfigurationEntityMapper(existingInputEntity.getFeaturePack()).apply(inputConfigurationDto));
    }

    private class ConfigValidator {

        ConfigValidator nameProvided(final InputConfigurationDto inputConfigurationDto) {
            if (StringUtils.isEmpty(inputConfigurationDto.getName())) {
                throw new DRServiceException(BAD_REQUEST_PARAM, "input configuration name is not set");
            }
            return this;
        }

        ConfigValidator nameUnique(final InputConfigurationDto inputConfigurationDto, final String featurePackId) {
            inputConfigurationsRepository.findByNameAndFeaturePackId(inputConfigurationDto.getName(), Long.parseLong(featurePackId))
                    .ifPresent(inputConfig -> {
                        throw new DRServiceException(CONFIG_EXISTS, inputConfig.getName(), inputConfig.getId().toString());
                    });
            return this;
        }

        ConfigValidator writeEnabled(final InputConfigurationEntity inputConfigurationEntity) {
            if (inputConfigurationEntity.isReadOnly()) {
                throw new DRServiceException(READ_ONLY_ACCESS, String.valueOf(inputConfigurationEntity.getId()));
            }
            return this;
        }

        ConfigValidator namesMatch(final InputConfigurationDto inputConfigurationDto, final InputConfigurationEntity inputConfigurationEntity) {
            if (!inputConfigurationDto.getName().equalsIgnoreCase(inputConfigurationEntity.getName())) {
                throw new DRServiceException(CONFIG_NAME_MISMATCH, inputConfigurationDto.getName(), inputConfigurationEntity.getName());
            }
            return this;
        }
    }

}