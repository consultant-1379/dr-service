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

import java.util.HashMap;
import java.util.Map;

import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto;
import org.springframework.stereotype.Component;

/**
 * Groovy script condition executed in the context of the target object.
 */
@Component
public class TargetScriptCondition extends ScriptCondition {

    @Override
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx) {
        final Map<String, Object> scriptArgs = new HashMap<>();
        scriptArgs.put("inputs", filterCtx.getInputs());
        scriptArgs.put("target", filterCtx.getSourceProperties());
        scriptArgs.put("sources", filterCtx.getTargetsProperties());
        return test(filterDef, filterCtx, scriptArgs);
    }

    @Override
    public boolean supports(ApplicationConfigurationFilterDto filterDef) {
        return filterDef.getCondition() != null ? FilterConditionDto.NameEnum.TARGETSCRIPT.equals(filterDef.getCondition().getName()) : false;
    }

    @Override
    public TYPE getType() {
        return TYPE.TARGET;
    }
}