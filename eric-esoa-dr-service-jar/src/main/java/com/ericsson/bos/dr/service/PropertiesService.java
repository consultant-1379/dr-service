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
package com.ericsson.bos.dr.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ericsson.bos.dr.jpa.PropertiesRepository;
import com.ericsson.bos.dr.jpa.model.PropertiesEntity;
import com.ericsson.bos.dr.web.v1.api.model.PropertyDto;

/**
 * Properties service.
 */
@Service
public class PropertiesService {

    @Autowired
    private PropertiesRepository propertiesRepository;

    /**
     * Return properties for a given feature pack. The properties
     * are put in a cache if not already present and will remain in the cache unless the
     * configured cache access timeout has expired for the entry.
     *
     * @param featurePackId
     *         feature pack id
     * @return feature pack properties
     */
    @Cacheable(value = "fp_properties_cache", key = "#featurePackId")
    public Map<String, Object> getProperties(final long featurePackId) {
        final Optional<PropertiesEntity> propertiesEntity =
                propertiesRepository.findByFeaturePackId(featurePackId);
        return propertiesEntity.isPresent() ?
                propertiesEntity.get().getConfig().getProperties()
                        .stream()
                        .collect(Collectors.toMap(PropertyDto::getName, PropertyDto::getValue)) :
                Collections.emptyMap();
    }
}
