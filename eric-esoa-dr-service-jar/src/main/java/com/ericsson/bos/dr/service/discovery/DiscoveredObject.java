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
package com.ericsson.bos.dr.service.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An object discovered on the source or target system.
 */
public class DiscoveredObject {

    /**
     * The type of discovered object.
     */
    public enum TYPE {SOURCE, TARGET}

    private final long jobId;
    private final TYPE type;
    private final Map<String, Object> properties;

    private Map<String, Object> additionalProperties = Collections.emptyMap();
    private final List<FilterResult> filterResults = new ArrayList<>();

    /**
     * DiscoveredObject.
     * @param  jobId job id
     * @param type source or target system
     * @param properties the discovered object properties
     */
    public DiscoveredObject(long jobId, TYPE type, Map<String, Object> properties) {
        this.jobId = jobId;
        this.type = type;
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public TYPE getType() {
        return type;
    }

    public long getJobId() {
        return jobId;
    }

    /**
     * Update the object properties. Any existing keys will be replaced by the updated value.
     * @param updates updated properties
     */
    public void updateProperties(Map<String, Object> updates) {
        properties.putAll(updates);
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(final Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public List<FilterResult> getFilterResults() {
        return filterResults;
    }

    /**
     * Add filter result.
     * @param filterResult filter result
     */
    public void addFilterResult(FilterResult filterResult) {
        this.filterResults.add(filterResult);
    }

    /**
     * Check if any filter has been matched.
     * @return true if any filter is matched
     */
    public boolean hasFilterMatch() {
        return filterResults.stream().anyMatch(FilterResult::isMatched);
    }

    @Override
    public String toString() {
        return "DiscoveredObject{" +
               "jobId=" + jobId +
               ", type=" + type +
               ", properties=" + properties +
               ", filterResults=" + filterResults +
               '}';
    }
}