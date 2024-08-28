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

import java.util.Map;

/**
 * Map discovered objects sort string to <code>Sort</code>.
 */
public class DiscoveredObjectSortMapper extends SortMapper {

    @Override
    public Map<String, String> getSortingAttributesMap() {
        return new DiscoveredObjectProperties().getSortable();
    }

    @Override
    public String getDefaultSort() {
        return "+objectId";
    }
}