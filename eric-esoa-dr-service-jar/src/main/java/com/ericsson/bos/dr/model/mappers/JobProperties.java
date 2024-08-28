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

import com.ericsson.bos.dr.jpa.model.JobEntity;

/**
 * Identifies the filterable and sortable properties for a Job, along
 * with the mapped database property name where applicable.
 */
public class JobProperties implements FilterableAndSortableProperties<JobEntity> {

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

    private static Map<String, String> collect(final Predicate<JobProperty> predicate) {
        return Arrays.stream(JobProperty.values())
                .filter(predicate)
                .collect(Collectors.toMap(prop -> prop.name, prop -> prop.mappedName));
    }

    public enum JobProperty {
        ID(true, true, "id", "id"),
        NAME(true, true, "name", "jobSpecification.name"),
        DESCRIPTION(true, true, "description", "jobSpecification.description"),
        STATUS(true, true, "status", "jobStatus"),
        START_DATE(true, false, "startDate", "startDate"),
        FP_ID(true, true, "featurePackId", "jobSpecification.featurePackId"),
        FP_NAME(true, true, "featurePackName", "jobSpecification.featurePackName"),
        APP_ID(true, true, "applicationId", "jobSpecification.applicationId"),
        APP_NAME(true, true, "applicationName", "jobSpecification.applicationName"),
        APP_JOB_NAME(true, true, "applicationJobName", "jobSpecification.applicationJobName"),
        JOB_SCHEDULE_ID(true, true, "jobScheduleId", "jobScheduleId");

        private final boolean isSortable;
        private final boolean isFilterable;
        private final String name;
        private final String mappedName;

        JobProperty(boolean isSortable, boolean isFilterable, final String name, final String mappedName) {
            this.isSortable = isSortable;
            this.isFilterable = isFilterable;
            this.name = name;
            this.mappedName = mappedName;
        }

        public String getMappedName() {
            return this.mappedName;
        }
    }
}