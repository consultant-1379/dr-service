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

import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackSummaryDto;

/**
 * Map <code>FeaturePackEntity</code> to <code>FeaturePackSummaryDto</code>.
 */
public class FeaturePackSummaryMapper implements Mapper<FeaturePackEntity, FeaturePackSummaryDto> {

    @Override
    public FeaturePackSummaryDto apply(FeaturePackEntity featurePack) {
        final var featurePackDto = new FeaturePackSummaryDto();
        featurePackDto.setId(String.valueOf(featurePack.getId()));
        featurePackDto.setName(featurePack.getName());
        featurePackDto.setDescription(featurePack.getDescription());
        featurePackDto.setCreatedAt(featurePack.getCreationDate().toInstant().toString());
        featurePackDto.setModifiedAt(featurePack.getModifiedDate().toInstant().toString());
        return featurePackDto;
    }
}
