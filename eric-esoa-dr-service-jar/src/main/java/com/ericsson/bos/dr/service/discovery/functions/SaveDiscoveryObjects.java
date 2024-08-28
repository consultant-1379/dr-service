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
package com.ericsson.bos.dr.service.discovery.functions;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.DiscoveryObjectRepository;
import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.model.mappers.DiscoveryObjectEntityMapper;
import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Persist the discovered source and target objects which have matched at least one filter.
 */
@Component
@DiscoveryFunction
public class SaveDiscoveryObjects implements Consumer<DiscoveryContext> {

    @Autowired
    private DiscoveryObjectRepository discoveryObjectRepository;

    @Override
    public void accept(DiscoveryContext discoveryContext) {
        final List<DiscoveryObjectEntity> sourceEntities =
                getDiscoveryObjectEntitiesWithFilterMatch(discoveryContext.getSources());
        discoveryObjectRepository.saveAll(sourceEntities);
        final List<DiscoveryObjectEntity> targetEntities =
                getDiscoveryObjectEntitiesWithFilterMatch(discoveryContext.getTargets());
        discoveryObjectRepository.saveAll(targetEntities);
    }

    private List<DiscoveryObjectEntity> getDiscoveryObjectEntitiesWithFilterMatch(final List<DiscoveredObject> discoveredObjects) {
        return discoveredObjects.stream()
                .filter(DiscoveredObject::hasFilterMatch) // exclude discovered objects which did not match any filter
                .map(o -> new DiscoveryObjectEntityMapper().apply(o))
                .collect(Collectors.toList());
    }
}