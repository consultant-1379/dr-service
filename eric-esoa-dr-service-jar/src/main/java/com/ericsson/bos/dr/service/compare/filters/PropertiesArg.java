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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Condition properties argument defining a sequence of source and target object properties to be matched.
 * The properties are defined in the format 'sourceProp1:targetProp1&sourceProp2:targetProp2'
 */
public class PropertiesArg {

    private static final String PROPERTIES_SEPARATOR = "&";
    private static final String PROPERTY_SEPARATOR = ":";

    private final List<String[]> propertiesParts;

    /**
     * PropertiesArg.
     * @param arg condition arg
     */
    public PropertiesArg(final String arg) {
        final String[] parts = arg.split(PROPERTIES_SEPARATOR);
        // Note: using linkedlist because targetArgs and sourceArgs lists in LinkSourceAndTarget
        // need to have the elements in same order, so the algorithm works correctly.
        this.propertiesParts = Arrays.stream(parts).map(p -> p.split(PROPERTY_SEPARATOR)).collect(Collectors.toCollection(LinkedList::new));
        this.propertiesParts.stream().forEach(p -> { assert p.length == 2: "Invalid arg: " + Arrays.toString(p);});
    }

    /**
     * Get properties parts. Each part is an array where the first index is the source property and the second
     * index is the target property.
     * @return parts
     */
    public List<String[]> getParts() {
        return this.propertiesParts;
    }

    /**
     * Get list of source args.
     * @return list of source args
     */
    public List<String> getSourceArgs() {
        return getParts().stream()
                .map(p -> p[0])
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get list of target args.
     * @return list of target args
     */
    public List<String> getTargetArgs() {
        return getParts().stream()
                .map(p -> p[1])
                .collect(Collectors.toCollection(LinkedList::new));
    }
}