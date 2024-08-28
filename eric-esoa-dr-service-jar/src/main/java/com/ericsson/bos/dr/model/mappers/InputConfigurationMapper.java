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

import com.ericsson.bos.dr.jpa.model.InputConfigurationEntity;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;

/**
 * Map <code>InputEntity</code> to <code>InputConfigurationDto</code>.
 */
public class InputConfigurationMapper implements Mapper<InputConfigurationEntity, InputConfigurationDto> {

    @Override
    public InputConfigurationDto apply(InputConfigurationEntity inputConfigurationEntity) {
        final var inputsConfigurationDto = inputConfigurationEntity.getConfig();
        inputsConfigurationDto.setId(String.valueOf(inputConfigurationEntity.getId()));
        return inputsConfigurationDto;
    }
}