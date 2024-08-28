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

import com.ericsson.bos.dr.jpa.model.ApplicationEntity;
import com.ericsson.bos.dr.web.v1.api.model.ConfigurationSummaryDto;

/**
 * Map <code>ApplicationEntity</code> to <code>ConfigurationSummaryDto</code>.
 */
public class ApplicationSummaryMapper implements Mapper<ApplicationEntity, ConfigurationSummaryDto> {

    @Override
    public ConfigurationSummaryDto apply(ApplicationEntity applicationEntity) {
        final var configurationSummaryDto = new ConfigurationSummaryDto();
        configurationSummaryDto.setId(String.valueOf(applicationEntity.getId()));
        configurationSummaryDto.setName(applicationEntity.getName());
        configurationSummaryDto.setDescription(applicationEntity.getDescription());
        return configurationSummaryDto;
    }
}