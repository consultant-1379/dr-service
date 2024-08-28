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

package com.ericsson.bos.dr.web;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.bos.dr.service.InputConfigurationsService;
import com.ericsson.bos.dr.web.v1.api.InputConfigurationsApi;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationListDto;
import com.ericsson.bos.dr.web.v1.api.model.InputConfigurationDto;

/**
 * Input configuration management controller.
 */
@RestController
public class InputConfigurationsController implements InputConfigurationsApi {

    @Autowired
    private InputConfigurationsService inputConfigurationsService;

    @Override
    public ResponseEntity<Void> createInputConfiguration(final String featurePackId, final InputConfigurationDto inputConfigurationDto) {
        inputConfigurationsService.createInputConfiguration(featurePackId, inputConfigurationDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteInputConfiguration(final String featurePackId, final String configurationId) {
        inputConfigurationsService.deleteInputConfiguration(featurePackId, configurationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<InputConfigurationDto> getInputConfiguration(final String featurePackId, final String configurationId,
            final Boolean evaluateFunctions) {
        return ResponseEntity.ok(inputConfigurationsService.getInputConfiguration(featurePackId, configurationId,
                Optional.ofNullable(evaluateFunctions).orElse(false)));
    }

    @Override
    public ResponseEntity<ConfigurationListDto> getInputConfigurations(final String featurePackId) {
        return ResponseEntity.ok(inputConfigurationsService.getInputConfigurations(featurePackId));
    }

    @Override
    public ResponseEntity<Void> updateInputConfiguration(final String featurePackId, final String configurationId,
            final InputConfigurationDto inputConfigurationDto) {
        inputConfigurationsService.replaceInputConfiguration(featurePackId, configurationId, inputConfigurationDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
