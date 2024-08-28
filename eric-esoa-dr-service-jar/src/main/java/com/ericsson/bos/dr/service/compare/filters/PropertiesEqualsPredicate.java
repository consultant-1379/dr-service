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

package com.ericsson.bos.dr.service.compare.filters;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Predicate to check equality of source and target property.
 */
public class PropertiesEqualsPredicate implements BiPredicate<String[], Map<String, Object>> {

    private final Map<String, Object> sourceProperties;

    /**
     * PropertiesEqualsPredicate
     * @param sourceProperties source properties
     */
    public PropertiesEqualsPredicate(Map<String, Object> sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    @Override
    public boolean test(String[] keys, Map<String, Object> targetProperties) {
        if (sourceProperties == null || targetProperties == null) {
            return false;
        }
        final String sourceKey = keys[0];
        final String targetKey = keys[1];
        if (sourceKey == null || targetKey == null) {
            return false;
        }
        final Object sourceValue = sourceProperties.get(sourceKey);
        final Object targetValue = targetProperties.get(targetKey);

        return Objects.equals(sourceValue, targetValue)
                || String.valueOf(sourceValue).equals(String.valueOf(targetValue));
    }
}