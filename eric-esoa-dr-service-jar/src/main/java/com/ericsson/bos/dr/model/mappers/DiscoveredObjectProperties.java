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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.model.DiscoveryObjectEntity;
import com.ericsson.bos.dr.jpa.specifications.DefaultPropertyFilterSpecification;
import com.ericsson.bos.dr.jpa.specifications.DiscoveredObjectPropertyFilterSpecification;
import com.ericsson.bos.dr.jpa.specifications.PropertyFilterSpecification;

/**
 * Identifies the filterable and sortable properties for a Discovered Object, along
 * with the mapped database property name where applicable.
 */
public class DiscoveredObjectProperties implements FilterableAndSortableProperties<DiscoveryObjectEntity> {

    private static final String PROPERTIES_MAP_NAME = "properties";

    @Override
    public boolean isFilterable(final String name) {
        return isInPropertiesMap(name) ||
               collect(prop -> prop.isFilterable)
                       .containsKey(name);
    }

    @Override
    public String getMappedName(final String name) {
        if (isInPropertiesMap(name)) {
            return name;
        } else {
            return collect(prop -> true).get(name);
        }
    }

    @Override
    public PropertyFilterSpecification<DiscoveryObjectEntity> getFilterSpecification(final String name) {
        final String actualName = isInPropertiesMap(name) ? PROPERTIES_MAP_NAME : name;
        return Arrays.stream(DiscoveredObjectProperty.values())
                .filter(p -> actualName.equalsIgnoreCase(p.name))
                .map(p -> p.customFilterSpecification)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(new DefaultPropertyFilterSpecification<>());
    }

    @Override
    public Map<String, String> getSortable() {
        return collect(prop -> prop.isSortable);
    }

    private Map<String, String> collect(final Predicate<DiscoveredObjectProperty> predicate) {
        return Arrays.stream(DiscoveredObjectProperty.values())
                .filter(predicate)
                .collect(Collectors.toMap(fp -> fp.name, fp -> fp.mappedName));
    }

    private boolean isInPropertiesMap(String name) {
        return name.startsWith("properties.");
    }

    private enum DiscoveredObjectProperty {
        ID(true, true, "objectId", "id", null),
        STATUS(true, true, "status", "status", null),
        PROPERTIES(false, true, PROPERTIES_MAP_NAME, PROPERTIES_MAP_NAME, new DiscoveredObjectPropertyFilterSpecification());

        private final boolean isSortable;
        private final boolean isFilterable;
        private final String name;
        private final String mappedName;
        private final PropertyFilterSpecification<DiscoveryObjectEntity> customFilterSpecification;

        DiscoveredObjectProperty(final boolean isSortable, final boolean isFilterable, final String name, final String mappedName,
                final PropertyFilterSpecification<DiscoveryObjectEntity> specification) {
            this.isSortable = isSortable;
            this.isFilterable = isFilterable;
            this.name = name;
            this.mappedName = mappedName;
            this.customFilterSpecification = specification;
        }
    }

}