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

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

/**
 * Map <code>sort</code> from request body to Sort, which is used in creating the page request
 */
public abstract class SortMapper implements Mapper<String, Sort> {

    private static final String DIRECTION_ASC = "+";
    private static final String DIRECTION_DESC = "-";
    private static final String DEFAULT_SORT_PARAM = "+id";

    @Override
    public Sort apply(String sortQueryParam) {
        final var sort = StringUtils.isEmpty(sortQueryParam) ? getDefaultSort() : sortQueryParam.trim();
        final var direction = getSortDirection(sort.substring(0, 1));
        final var sortAttribute = containsDirection(sort) ? sort.substring(1) : sort;

        final Map<String, String> sortAttributeMappings = getSortingAttributesMap();
        if (sortAttributeMappings.containsKey(sortAttribute)) {
            return Sort.by(direction, sortAttributeMappings.get(sortAttribute));
        }

        throw new DRServiceException(ErrorCode.INVALID_SORTING_PARAM, sortAttribute);
    }

    /**
     * Get a map of sorting attributes
     *
     * @return map of sorting attributes
     */
    protected abstract Map<String, String> getSortingAttributesMap();

    protected String getDefaultSort() {
        return DEFAULT_SORT_PARAM;
    }

    private Sort.Direction getSortDirection(String direction) {
        if ((DIRECTION_ASC).equals(direction)) {
            return Sort.Direction.ASC;
        } else if ((DIRECTION_DESC).equals(direction)) {
            return Sort.Direction.DESC;
        } else {
            return Sort.DEFAULT_DIRECTION;
        }
    }

    private boolean containsDirection(final String sort) {
        return sort.startsWith(DIRECTION_ASC) || sort.startsWith(DIRECTION_DESC);
    }
}