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

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;

/**
 * The result of a comparison filter applied to a <code>DiscoveryObject</code>.
 */
public class FilterResult {

    private final String name;
    private ApplicationConfigurationFilterDto filterDef;
    private final boolean matched;

    /**
     * FilterResult.
     * @param name the filter name
     * @param matched true if filter resulted in a match with the source object
     * @param filterDef the configured discrepancy text
     */
    public FilterResult(String name, ApplicationConfigurationFilterDto filterDef, boolean matched) {
        this.name = name;
        this.filterDef = filterDef;
        this.matched = matched;
    }

    public String getName() {
        return name;
    }

    public String getDiscrepency() {
        return filterDef.getFilterMatchText();
    }

    public boolean isMatched() {
        return matched;
    }

    public ApplicationConfigurationFilterDto getFilterDef() {
        return filterDef;
    }
}
