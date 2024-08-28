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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum;
import com.ericsson.bos.dr.web.v1.api.model.FilterDto;
import com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction;

/**
 * Map <code>DiscoveryObjectEntity</code> to <code>DiscoveredObjectDto</code>.
 * <p>
 * The properties in the  <code>DiscoveryObjectEntity</code> are filtered to include
 * only those properties defined as outputs in the job configuration.
 * Both the source and target properties will be included, where the source property
 * will take precedence is case of name conflict.
 * </p>
 */
public class DiscoveryObjectMapper implements Mapper<DiscoveryObjectEntity, DiscoveredObjectDto> {

    private final List<String> apiPropertyNames;

    /**
     * DiscoveryObjectMapper.
     *
     * @param apiPropertyNames api property names
     */
    public DiscoveryObjectMapper(final List<String> apiPropertyNames) {
        this.apiPropertyNames = apiPropertyNames;
    }

    @Override
    public DiscoveredObjectDto apply(final DiscoveryObjectEntity discoveryObjectEntity) {
        final var dto = new DiscoveredObjectDto();
        dto.setObjectId(String.valueOf(discoveryObjectEntity.getId()));
        final Map<String, Object> properties = new HashMap<>();
        properties.putAll(ApiPropertiesFilter.filterTargetProperties(apiPropertyNames, discoveryObjectEntity.getTargetProperties()));
        properties.putAll(ApiPropertiesFilter.filterSourceProperties(apiPropertyNames, discoveryObjectEntity.getSourceProperties()));
        dto.setProperties(properties);
        Optional.ofNullable(discoveryObjectEntity.getErrorMessage()).ifPresent(dto::setErrorMessage);
        dto.setStatus(StatusEnum.valueOf(discoveryObjectEntity.getStatus().toUpperCase()));
        discoveryObjectEntity.getFilters().forEach(f -> {
            final var filterDto = new FilterDto();
            filterDto.setName(f.getName());
            final var actionDto = new FilterDtoReconcileAction();
            actionDto.setName(f.getReconcileAction());
            actionDto.setStatus(FilterDtoReconcileAction.StatusEnum.valueOf(f.getReconcileStatus().toUpperCase()));
            Optional.ofNullable(f.getCommand()).ifPresent(actionDto::setCommand);
            Optional.ofNullable(f.getCommandResponse()).ifPresent(actionDto::setCommandOutput);
            Optional.ofNullable(f.getErrorMsg()).ifPresent(actionDto::setErrorMessage);
            filterDto.setReconcileAction(actionDto);
            dto.addFiltersItem(filterDto);
            dto.addDiscrepanciesItem(f.getDiscrepancy());
        });
        return dto;
    }
}