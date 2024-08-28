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

import static com.ericsson.bos.dr.service.exceptions.ErrorCode.NON_UNIQUE_SOURCE_TARGET_MAPPING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.ericsson.bos.dr.service.compare.filters.PropertiesArg;
import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.service.discovery.DiscoveryContext;
import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.compare.filters.MultiKeyBuilder;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Link source and target objects as specified in the 'linkSourceAndTarget' tag
 * in the application configuration. The target properties are set in the mapped source.
 */
@Component
@DiscoveryFunction
public class LinkSourceAndTarget implements Consumer<DiscoveryContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkSourceAndTarget.class);

    @Autowired
    private MultiKeyBuilder multiKeyBuilder;

    @Override
    public void accept(final DiscoveryContext discoveryContext) {
        if (StringUtils.isEmpty(discoveryContext.getSourceAndTargetsLink())) {
            return;
        }
        LOGGER.info("Linking sources and targets for jobId={}", discoveryContext.getJobId());
        final var propertiesArg = new PropertiesArg(discoveryContext.getSourceAndTargetsLink());
        final List<String> targetArgs = propertiesArg.getTargetArgs();
        final List<String> sourceArgs = propertiesArg.getSourceArgs();
        final MultiValuedMap<MultiKey<Object>, DiscoveredObject> targetsByValueOfTargetArgs = new ArrayListValuedHashMap<>();
        for (final DiscoveredObject target : discoveryContext.getTargets()) {
            final Optional<MultiKey<Object>>  multiKey =  multiKeyBuilder.build(target.getProperties(), targetArgs);
            multiKey.ifPresent(mk -> targetsByValueOfTargetArgs.put(mk, target));
        }
        for (final DiscoveredObject source : discoveryContext.getSources()) {
            final Optional<MultiKey<Object>> multiKey = multiKeyBuilder.build(source.getProperties(), sourceArgs);
            final Collection<DiscoveredObject> matchedTargets = new ArrayList<>();
            multiKey.ifPresent(objectMultiKey -> matchedTargets.addAll(targetsByValueOfTargetArgs.get(objectMultiKey)));
            if (matchedTargets.size() > 1) {
                LOGGER.error("Multiple targets are mapped to the same source. "
                          + "Source: {} Targets: {} Mappings: {}", source, matchedTargets, discoveryContext.getSourceAndTargetsLink());
                throw new DRServiceException(
                        NON_UNIQUE_SOURCE_TARGET_MAPPING, discoveryContext.getSourceAndTargetsLink());
            }
            for (final DiscoveredObject target : matchedTargets) {
                source.setAdditionalProperties(target.getProperties());
                target.setAdditionalProperties(source.getProperties());
            }
        }
    }

}