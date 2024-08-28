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

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.jpa.model.FilterEntity;
import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.FilterResult;
import com.ericsson.bos.dr.web.v1.api.model.DiscoveredObjectDto.StatusEnum;
import com.ericsson.bos.dr.web.v1.api.model.FilterDtoReconcileAction;

/**
 * Map <code>DiscoveredObject</code> to <code>DiscoveryObjectEntity</code>.
 * <p>
 * For a discovered object on the source system, the source properties and optionally target
 * properties will be set if source and target have been linked.
 * For a discovered object on the target system only the target properties will be set.
 * </p>
 */
public class DiscoveryObjectEntityMapper implements Mapper<DiscoveredObject, DiscoveryObjectEntity> {

    @Override
    public DiscoveryObjectEntity apply(final DiscoveredObject discoveredObject) {
        final var entity = new DiscoveryObjectEntity();
        entity.setJobId(discoveredObject.getJobId());

        if (DiscoveredObject.TYPE.SOURCE.equals(discoveredObject.getType())) {
            entity.setSourceProperties(discoveredObject.getProperties());
            entity.setTargetProperties(discoveredObject.getAdditionalProperties());
        } else {
            entity.setTargetProperties(discoveredObject.getProperties());
        }
        entity.setStatus(StatusEnum.DISCOVERED.toString());
        discoveredObject.getFilterResults().stream()
                .filter(FilterResult::isMatched) // exclude filters which were not matched
                .forEach(f -> {
                    final var filterEntity = new FilterEntity();
                    filterEntity.setName(f.getName());
                    filterEntity.setDiscrepancy(f.getDiscrepency());
                    filterEntity.setReconcileAction(f.getFilterDef().getReconcileAction());
                    filterEntity.setReconcileStatus(FilterDtoReconcileAction.StatusEnum.NOT_STARTED.toString());
                    entity.addFilter(filterEntity);
                });
        return entity;
    }
}