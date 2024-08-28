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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;


/**
 * Filter source and target discovered object properties to return only those properties defined in the API properties
 * configuration for the job.
 */
public class ApiPropertiesFilter {

    private ApiPropertiesFilter() {}

    /**
     * Filter source properties to return only those defined in the api properties.
     * @param apiPropertyNames api property names
     * @param discoveredObectProperties discovered object source properties
     * @return filtered api properties
     */
    public static Map<String, Object> filterSourceProperties(final List<String> apiPropertyNames,
                                                             final Map<String, Object> discoveredObectProperties) {
        return filterApiProperties(apiPropertyNames, discoveredObectProperties, "source.");
    }

    private static Map<String, Object> filterApiProperties(final List<String> apiPropertyNames,
                                                           final Map<String, Object> discoveredObectProperties, String namePrefix) {
        if (CollectionUtils.isEmpty(apiPropertyNames)) {
            return discoveredObectProperties;
        }
        if (MapUtils.isEmpty(discoveredObectProperties)) {
            return Collections.emptyMap();
        }
        final Map<String, Object> filteredProperties = new HashMap<>();
        for (final String apiPropertyName: apiPropertyNames) {
            final var value = discoveredObectProperties.containsKey(apiPropertyName) ?
                    discoveredObectProperties.get(apiPropertyName) :
                    discoveredObectProperties.get(apiPropertyName.replace(namePrefix, ""));
            if (value != null) {
                filteredProperties.put(apiPropertyName, value);
            }
        }
        return filteredProperties;
    }

    /**
     * Filter target properties to return only those defined in the api properties.
     * @param apiPropertyNames api property names
     * @param discoveredObectProperties discovered object target properties
     * @return filtered api properties
     */
    public static Map<String, Object> filterTargetProperties(final List<String> apiPropertyNames,
                                                             final Map<String, Object> discoveredObectProperties) {
        return filterApiProperties(apiPropertyNames, discoveredObectProperties, "target.");
    }
}