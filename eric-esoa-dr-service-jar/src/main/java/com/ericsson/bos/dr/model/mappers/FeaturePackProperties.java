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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.jpa.model.FeaturePackEntity;

/**
 * Identifies the filterable and sortable properties for a FeaturePack, along
 * with the mapped database property name where applicable.
 */
public class FeaturePackProperties implements FilterableAndSortableProperties<FeaturePackEntity> {

    @Override
    public boolean isFilterable(final String propertyName) {
        return collect(prop -> prop.isFilterable)
                .containsKey(propertyName);
    }

    @Override
    public String getMappedName(final String name) {
        return collect(prop -> true).get(name);
    }

    @Override
    public Map<String, String> getSortable() {
        return collect(prop -> prop.isSortable);
    }

    private Map<String, String> collect(final Predicate<FeaturePackProperty> predicate) {
        return Arrays.stream(FeaturePackProperty.values())
                .filter(predicate)
                .collect(Collectors.toMap(fp -> fp.name, fp -> fp.mappedName));
    }

    private enum FeaturePackProperty {
        ID(true, true, "id", "id"),
        NAME(true, true, "name", "name"),
        DESCRIPTION(true, true, "description", "description"),
        CREATED_AT(true, false, "createdAt", "creationDate"),
        MODIFIED_AT(true, false, "modifiedAt", "modifiedDate");

        private final boolean isSortable;
        private final boolean isFilterable;
        private final String name;
        private final String mappedName;

        FeaturePackProperty(boolean isSortable, boolean isFilterable, String name, String mappedName) {
            this.isSortable = isSortable;
            this.isFilterable = isFilterable;
            this.name = name;
            this.mappedName = mappedName;
        }
    }
}
