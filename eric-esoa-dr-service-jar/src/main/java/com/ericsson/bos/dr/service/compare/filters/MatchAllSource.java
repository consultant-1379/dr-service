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
import org.springframework.stereotype.Component;

/**
 * Match all source filter condition which will be applied if the filter definition does not
 * contain any conditions.
 */
@Component
public class MatchAllSource implements Condition {

    @Override
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx) {
        return true;
    }

    @Override
    public boolean supports(ApplicationConfigurationFilterDto filterDef) {
        return filterDef.getCondition() == null;
    }

    @Override
    public TYPE getType() {
        return TYPE.SOURCE;
    }
}