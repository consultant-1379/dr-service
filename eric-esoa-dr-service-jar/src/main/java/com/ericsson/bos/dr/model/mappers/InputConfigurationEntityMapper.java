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
package com.ericsson.bos.dr.model.mappers;

import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.jpa.model.InputConfigurationEntity;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;

/**
 * Map <code>InputConfigurationDto</code> to <code>InputEntity</code>.
 */
public class InputConfigurationEntityMapper implements Mapper<InputConfigurationDto, InputConfigurationEntity> {

    private FeaturePackEntity featurePackEntity;

    /**
     * InputsEntityMapper
     * @param featurePackEntity associated feature pack entity
     */
    public InputConfigurationEntityMapper(final FeaturePackEntity featurePackEntity) {
        this.featurePackEntity = featurePackEntity;
    }

    @Override
    public InputConfigurationEntity apply(InputConfigurationDto inputConfigurationDto) {
        final var inputsEntity = new InputConfigurationEntity();
        inputsEntity.setName(inputConfigurationDto.getName());
        Optional.ofNullable(inputConfigurationDto.getDescription()).ifPresent(inputsEntity::setDescription);
        inputsEntity.setConfig(inputConfigurationDto);
        inputsEntity.setReadOnly(false);
        inputsEntity.setFilename(inputConfigurationDto.getName() + ".yml");
        inputsEntity.setFeaturePack(featurePackEntity);
        return inputsEntity;
    }
}