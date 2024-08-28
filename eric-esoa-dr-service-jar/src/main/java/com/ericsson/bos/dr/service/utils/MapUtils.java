
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
package com.ericsson.bos.dr.service.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Map utilities.
 */
public class MapUtils {

    private MapUtils(){}

    /**
     * Merge two maps. Values in fromMap will override values in toMap.
     * Will not override to map with null values.
     * Original maps are unchanged.
     *
     * @param fromMap
     *         map to merge from
     * @param toMap
     *         map to merge to
     * @return new map containing merge
     */
    public static Map<String, Object> merge(Map<String, Object> fromMap, Map<String, Object> toMap) {
        return Stream.of(Optional.ofNullable(toMap).orElse(new HashMap<>()),
                        Optional.ofNullable(fromMap).orElse(new HashMap<>()))
                .flatMap(map -> map.entrySet().stream())
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v2));
    }


}
