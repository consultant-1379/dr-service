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
 * Returns a map of sortable attributes for feature pack.
 * With as key the attribute name in the dto and as value the name in the entity.
 * This map is required as the attribute names in the dto can differ from the attribute names in the entity.
 *
 */
public class FeaturePackSortMapper extends SortMapper {

    @Override
    public Map<String, String> getSortingAttributesMap() {
        return new FeaturePackProperties().getSortable();
    }

}
