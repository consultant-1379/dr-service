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

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;

/**
 * Filter Condition to be applied to filter list of source or target objects, e.g <code>SourceNotInTarget</code> .
 */
public interface Condition {

    /**
     * Condition type, SOURCE or TARGET.
     */
    enum TYPE {SOURCE, TARGET}

    /**
     * Evaluate the filter condition against the supplied <code>FilterContext</code>.
     *
     * @param filterDef filter definition containing the condition and its args
     * @param filterCtx context containing the source object and target objects for evaluation
     * @return condition result, true or false
     */
    boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx);

    /**
     * Return true if <code>Condition</code> can evaluate the filter definition.
     * @param filterDef filter definition containing the condition and its args
     * @return true if supported, otherwise false
     */
    boolean supports(ApplicationConfigurationFilterDto filterDef);

    /**
     * Get the type of condition, SOURCE or TARGET.
     * @return TYPE
     */
    TYPE getType();
}