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

import com.ericsson.bos.dr.service.discovery.DiscoveredObject;
import com.ericsson.bos.dr.web.v1.api.model.ApplicationConfigurationFilterDto;
import com.ericsson.bos.dr.web.v1.api.model.FilterConditionDto;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

/**
 * Check for no matching target object in a list of source objects or a linked source object.
 * <p>
 * If no arg is supplied then checks if target has linked source. Returns true if the target has no linked
 * source.</p>
 * <p>
 * If arg is supplied and target is linked to a source then checks the target and linked source properties.
 * Returns true if the properties are not equal.</p>
 * <p>
 * If arg is supplied and there is no linked source object, then checks the full source list. If
 * no match is found, then returns true.
 * </p>
 */
@Component
class TargetNotInSource implements Condition {

    @Override
    public boolean test(ApplicationConfigurationFilterDto filterDef, FilterContext filterCtx) {
        final DiscoveredObject targetObject = filterCtx.getSource();
        final Map<String, Object> targetProperties = filterCtx.getSourceProperties();
        final List<Map<String, Object>> sources = filterCtx.getTargetsProperties();

        final var hasLinkedSource = MapUtils.isNotEmpty(targetObject.getAdditionalProperties());
        if (filterDef.getCondition().getArg() == null){
            return !hasLinkedSource;
        }

        final String arg = filterDef.getCondition().getArg();
        final var propertiesArg = new PropertiesArg(arg);

        if (hasLinkedSource) {
            return !propertiesArg.getParts().stream().allMatch(p ->
                    new PropertiesEqualsPredicate(targetObject.getAdditionalProperties()).test(p, targetProperties));
        } else {
            return sources.stream().noneMatch(source -> propertiesArg.getParts().stream()
                    .allMatch(p -> new PropertiesEqualsPredicate(source).test(p, targetProperties)));
        }
    }

    @Override
    public boolean supports(ApplicationConfigurationFilterDto filterDef) {
        return filterDef.getCondition() != null ? FilterConditionDto.NameEnum.TARGETNOTINSOURCE.equals(filterDef.getCondition().getName()) : false;
    }

    @Override
    public TYPE getType() {
        return TYPE.TARGET;
    }
}