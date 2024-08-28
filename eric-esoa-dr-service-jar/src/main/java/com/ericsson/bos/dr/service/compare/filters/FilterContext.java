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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;

/**
 * Contains the source object that is currently being compared against a filter.
 * Initially contains all the target objects. Target list may get reduced from
 * condition to condition.
 */
public class FilterContext {
    private final DiscoveredObject source;
    private final Map<String, Object> sourceProperties;
    private final List<Map<String, Object>> targetsProperties;
    private final Map<String, Object> inputs;
    private final long featurePackId;

    /**
     * FilterContext.
     *  @param source source object
     * @param targets target objects
     * @param featurePackId feature pack id
     * @param inputs job inputs
     */
    public FilterContext(final DiscoveredObject source, final List<DiscoveredObject> targets,
                         long featurePackId, final Map<String, Object> inputs) {
        this.source = source;
        this.sourceProperties = source.getProperties();
        this.targetsProperties = targets.stream().map(DiscoveredObject::getProperties).collect(Collectors.toList());
        this.featurePackId = featurePackId;
        this.inputs = inputs;
    }

    public DiscoveredObject getSource() {
        return this.source;
    }

    /**
     * Return source object properties.
     *
     * @return source object properties.
     */
    public Map<String, Object> getSourceProperties() {
        return sourceProperties;
    }

    /**
     * Return properties for each target.
     *
     * @return target list containing the properties of each target.
     */
    public List<Map<String, Object>> getTargetsProperties() {
        return targetsProperties;
    }

    public long getFeaturePackId() {
        return featurePackId;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }
}