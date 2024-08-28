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
 * Returns a map of sortable attributes for job schedule.
 * This map is required as the attribute names in the dto can differ from the attribute names in the entity.
 */
public class JobScheduleSortMapper extends SortMapper {

    @Override
    public Map<String, String> getSortingAttributesMap() {
        return new JobScheduleProperties().getSortable();
    }
}
