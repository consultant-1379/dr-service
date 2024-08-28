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

import java.util.Collections;

import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationSummaryDto;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto;

/**
 * Map <code>FeaturePackEntity</code> to <code>FeaturePackDto</code>.
 */
public class FeaturePackMapper implements Mapper<FeaturePackEntity, FeaturePackDto> {

    @Override
    public FeaturePackDto apply(FeaturePackEntity featurePack) {
        final var featurePackDto = new FeaturePackDto();
        featurePackDto.setId(String.valueOf(featurePack.getId()));
        featurePackDto.setName(featurePack.getName());
        featurePackDto.setDescription(featurePack.getDescription());
        featurePackDto.setCreatedAt(featurePack.getCreationDate().toInstant().toString());
        featurePackDto.setModifiedAt(featurePack.getModifiedDate().toInstant().toString());
        featurePack.getApplications().stream().forEach(app -> featurePackDto.addApplicationsItem(
                new ConfigurationSummaryDto()
                        .name(app.getName())
                        .description(app.getDescription())
                        .id(String.valueOf(app.getId()))));
        featurePack.getAssets().stream().forEach(asset -> featurePackDto.addAssetsItem(
                new ConfigurationSummaryDto()
                        .name(asset.getName())
                        .description(asset.getDescription())
                        .id(String.valueOf(asset.getId()))));
        featurePack.getProperties().ifPresentOrElse(p -> featurePackDto.setProperties(p.getConfig().getProperties()),
                () -> featurePackDto.setProperties(Collections.emptyList()));
        featurePack.getInputs().stream().forEach(inputs -> featurePackDto.addInputsItem(
                new ConfigurationSummaryDto()
                        .name(inputs.getName())
                        .description(inputs.getDescription())
                        .id(String.valueOf(inputs.getId()))));
        featurePack.getListeners().stream().forEach(listener -> featurePackDto.addListenersItem(
                new ConfigurationSummaryDto()
                        .name(listener.getName())
                        .description(listener.getDescription())
                        .id(String.valueOf(listener.getId()))));
        return featurePackDto;
    }
}